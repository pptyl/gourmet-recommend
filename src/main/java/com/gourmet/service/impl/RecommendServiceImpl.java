package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.redis.RedisLockUtil;
import com.gourmet.common.redis.RedisService;
import com.gourmet.common.result.ResultCode;
import com.gourmet.config.SeckillProperties;
import com.gourmet.entity.BrowseHistory;
import com.gourmet.entity.Favorite;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Note;
import com.gourmet.entity.RecommendFeedback;
import com.gourmet.entity.RecommendLog;
import com.gourmet.entity.Review;
import com.gourmet.entity.User;
import com.gourmet.mapper.BrowseHistoryMapper;
import com.gourmet.mapper.FavoriteMapper;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.NoteMapper;
import com.gourmet.mapper.RecommendFeedbackMapper;
import com.gourmet.mapper.RecommendLogMapper;
import com.gourmet.mapper.ReviewMapper;
import com.gourmet.mapper.UserMapper;
import com.gourmet.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 个性化推荐服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private static final int MAX_CANDIDATE = 300;
    private static final int CACHE_RANK_SIZE = 50;

    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final FavoriteMapper favoriteMapper;
    private final ReviewMapper reviewMapper;
    private final NoteMapper noteMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final RecommendFeedbackMapper feedbackMapper;
    private final RecommendLogMapper recommendLogMapper;
    private final RedisService redisService;
    private final RedisLockUtil redisLockUtil;
    private final SeckillProperties seckillProperties;

    @Override
    public List<Merchant> homeRecommend(Long userId, int limit) {
        int size = normalizeLimit(limit);
        List<Merchant> merchants = loadApproved();
        if (merchants.isEmpty()) {
            return List.of();
        }

        User user = userId == null ? null : userMapper.selectById(userId);
        UserBehavior behavior = userId == null ? new UserBehavior() : loadBehavior(userId);
        Set<String> tasteCuisines = user == null ? Set.of() : splitCsv(user.getTasteCuisine());

        boolean coldStart = tasteCuisines.isEmpty()
                && behavior.favoriteIds.isEmpty()
                && behavior.reviewedMerchantIds.isEmpty()
                && behavior.browsedMerchantIds.isEmpty()
                && behavior.likedMerchantIds.isEmpty();
        if (coldStart) {
            List<Merchant> result = coldStartInternal(merchants, size);
            writeLogs(userId, result, BizConstants.ALGO_COLD_START);
            return result;
        }

        // 不同行为来源的菜系权重
        Map<String, Double> cuisineWeights = new HashMap<>();
        tasteCuisines.forEach(c -> cuisineWeights.merge(c, 3.0, Double::sum));

        Map<Long, Merchant> merchantIndex = merchants.stream()
                .collect(Collectors.toMap(Merchant::getId, m -> m, (a, b) -> a));

        behavior.favoriteIds.forEach(id -> addCuisineWeight(cuisineWeights, merchantIndex.get(id), 2.0));
        behavior.reviewedMerchantIds.forEach(id -> addCuisineWeight(cuisineWeights, merchantIndex.get(id), 2.0));
        behavior.browsedMerchantIds.forEach(id -> addCuisineWeight(cuisineWeights, merchantIndex.get(id), 1.0));
        behavior.likedMerchantIds.forEach(id -> addCuisineWeight(cuisineWeights, merchantIndex.get(id), 2.0));

        List<Merchant> candidates = new ArrayList<>();
        for (Merchant merchant : merchants) {
            if (behavior.dislikedMerchantIds.contains(merchant.getId())) {
                continue;
            }
            double raw = baseScore(merchant)
                    + cuisineWeights.getOrDefault(merchant.getCategory(), 0.0) * 4.0
                    - (behavior.favoriteIds.contains(merchant.getId()) ? 6.0 : 0.0);
            merchant.setRecommendScore(raw);
            merchant.setRecommendAlgorithm(BizConstants.ALGO_PREFERENCE);
            merchant.setRecommendReason(buildReason(merchant, tasteCuisines, behavior, merchantIndex));
            candidates.add(merchant);
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        normalizeScores(candidates);
        List<Merchant> top = candidates.stream()
                .sorted(Comparator.comparing(Merchant::getRecommendScore).reversed())
                .limit(size)
                .toList();

        writeLogs(userId, top, BizConstants.ALGO_PREFERENCE);
        return top;
    }

    @Override
    public List<Merchant> similar(Long merchantId, int limit) {
        int size = normalizeLimit(limit);
        Merchant target = merchantMapper.selectById(merchantId);
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        }

        List<Merchant> candidates = loadApproved().stream()
                .filter(m -> !m.getId().equals(merchantId))
                .filter(m -> Objects.equals(m.getCategory(), target.getCategory()))
                .collect(Collectors.toList());

        // 同类不足时，用评分相近的其他商家补齐
        if (candidates.size() < size) {
            Set<Long> exists = candidates.stream().map(Merchant::getId).collect(Collectors.toSet());
            loadApproved().stream()
                    .filter(m -> !m.getId().equals(merchantId))
                    .filter(m -> !exists.contains(m.getId()))
                    .sorted(Comparator.comparingDouble((Merchant m) -> priceDiff(m, target)))
                    .limit((long) size - candidates.size())
                    .forEach(candidates::add);
        }

        double targetPrice = target.getAvgPrice() == null ? 0 : target.getAvgPrice().doubleValue();
        for (Merchant merchant : candidates) {
            double price = merchant.getAvgPrice() == null ? 0 : merchant.getAvgPrice().doubleValue();
            double priceRatio = targetPrice <= 0 ? 0 : Math.min(1.0, Math.abs(price - targetPrice) / targetPrice);
            double raw = 100 - priceRatio * 25 + baseScore(merchant) * 0.8;
            merchant.setRecommendScore(raw);
            merchant.setRecommendAlgorithm(BizConstants.ALGO_SIMILAR);
            merchant.setRecommendReason(Objects.equals(merchant.getCategory(), target.getCategory())
                    ? "与「" + target.getName() + "」同为" + target.getCategory() + "，人均相近"
                    : "综合评分相近的优质商家");
        }
        normalizeScores(candidates);

        List<Merchant> top = candidates.stream()
                .sorted(Comparator.comparing(Merchant::getRecommendScore).reversed())
                .limit(size)
                .toList();
        writeLogs(null, top, BizConstants.ALGO_SIMILAR);
        return top;
    }

    @Override
    public List<Merchant> hotRank(int limit) {
        int size = normalizeLimit(limit);
        String cacheKey = seckillProperties.getRankCacheKey();
        List<Merchant> cached = redisService.getJson(cacheKey, new TypeReference<List<Merchant>>() {
        });
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(size).toList();
        }

        // 分布式锁避免缓存重建时并发击穿数据库；拿不到锁就直接查库
        String lockKey = "lock:" + cacheKey;
        String lockToken = redisLockUtil.tryLock(lockKey, Duration.ofSeconds(10));
        try {
            if (lockToken != null) {
                cached = redisService.getJson(cacheKey, new TypeReference<List<Merchant>>() {
                });
                if (cached != null && !cached.isEmpty()) {
                    return cached.stream().limit(size).toList();
                }
            }

            List<Merchant> merchants = loadApproved();
            for (Merchant merchant : merchants) {
                merchant.setHeat(heatOf(merchant));
                merchant.setRecommendScore(merchant.getHeat());
                merchant.setRecommendAlgorithm(BizConstants.ALGO_HOT);
                merchant.setRecommendReason("综合评分、收藏量、评价数与浏览量加权人气榜");
            }
            List<Merchant> rank = merchants.stream()
                    .sorted(Comparator.comparing(Merchant::getHeat).reversed())
                    .limit(Math.min(size, CACHE_RANK_SIZE))
                    .toList();

            redisService.setJson(cacheKey, rank, Duration.ofSeconds(seckillProperties.getRankCacheSeconds()));
            return rank;
        } finally {
            redisLockUtil.unlock(lockKey, lockToken);
        }
    }

    @Override
    public List<Merchant> coldStart(int limit) {
        List<Merchant> result = coldStartInternal(loadApproved(), normalizeLimit(limit));
        writeLogs(null, result, BizConstants.ALGO_COLD_START);
        return result;
    }

    /* ------------------------------------------------------------------ */
    /*                            内部实现                                 */
    /* ------------------------------------------------------------------ */

    private List<Merchant> coldStartInternal(List<Merchant> merchants, int size) {
        if (merchants.isEmpty()) {
            return List.of();
        }
        // 先按热度取头部商家，再随机打散，兼顾"热门"与"新鲜感"
        List<Merchant> pool = merchants.stream()
                .sorted(Comparator.comparingDouble(this::heatOf).reversed())
                .limit(20)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(pool, new Random());

        List<Merchant> result = new ArrayList<>(pool.stream().limit(size).toList());
        double min = result.stream().mapToDouble(this::heatOf).min().orElse(0);
        double max = result.stream().mapToDouble(this::heatOf).max().orElse(0);
        for (Merchant merchant : result) {
            double normalized = max - min < 0.0001
                    ? 80.0
                    : 60 + (heatOf(merchant) - min) / (max - min) * 35;
            merchant.setRecommendAlgorithm(BizConstants.ALGO_COLD_START);
            merchant.setRecommendReason("新用户冷启动：热门商家随机推荐");
            merchant.setRecommendScore(round(normalized));
        }
        return result;
    }

    private List<Merchant> loadApproved() {
        return merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getAuditStatus, BizConstants.MERCHANT_AUDIT_PASSED)
                        .eq(Merchant::getStatus, BizConstants.MERCHANT_OPEN)
                        .orderByDesc(Merchant::getScore)
                        .last("limit " + MAX_CANDIDATE));
    }

    private UserBehavior loadBehavior(Long userId) {
        UserBehavior behavior = new UserBehavior();

        behavior.favoriteIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId))
                .stream().map(Favorite::getMerchantId).collect(Collectors.toSet());

        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getStatus, BizConstants.CONTENT_APPROVED));
        behavior.reviewedMerchantIds = reviews.stream()
                .filter(r -> r.getScore() != null && r.getScore() >= 4)
                .map(Review::getMerchantId).collect(Collectors.toSet());

        List<Note> notes = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .isNotNull(Note::getMerchantId));
        Set<Long> noteMerchantIds = notes.stream().map(Note::getMerchantId).collect(Collectors.toSet());

        List<BrowseHistory> histories = browseHistoryMapper.selectList(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .orderByDesc(BrowseHistory::getCreateTime)
                .last("limit 50"));
        behavior.browsedMerchantIds = histories.stream()
                .map(BrowseHistory::getMerchantId).collect(Collectors.toSet());
        behavior.browsedMerchantIds.addAll(noteMerchantIds);

        List<RecommendFeedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<RecommendFeedback>()
                .eq(RecommendFeedback::getUserId, userId));
        behavior.likedMerchantIds = feedbacks.stream()
                .filter(f -> f.getFeedbackType() != null && f.getFeedbackType() == BizConstants.FEEDBACK_LIKE)
                .map(RecommendFeedback::getMerchantId).collect(Collectors.toSet());
        behavior.dislikedMerchantIds = feedbacks.stream()
                .filter(f -> f.getFeedbackType() != null && f.getFeedbackType() == BizConstants.FEEDBACK_DISLIKE)
                .map(RecommendFeedback::getMerchantId).collect(Collectors.toSet());
        return behavior;
    }

    private String buildReason(Merchant merchant, Set<String> tasteCuisines,
                               UserBehavior behavior, Map<Long, Merchant> index) {
        if (merchant.getCategory() != null && tasteCuisines.contains(merchant.getCategory())) {
            return "口味偏好匹配：" + merchant.getCategory();
        }
        boolean likedSameCategory = behavior.likedMerchantIds.stream()
                .map(index::get).filter(Objects::nonNull)
                .anyMatch(m -> Objects.equals(m.getCategory(), merchant.getCategory()));
        if (likedSameCategory) {
            return "你反馈喜欢的同类美食";
        }
        boolean favoriteSameCategory = behavior.favoriteIds.stream()
                .map(index::get).filter(Objects::nonNull)
                .anyMatch(m -> Objects.equals(m.getCategory(), merchant.getCategory()));
        if (favoriteSameCategory) {
            return "你收藏过同类店铺";
        }
        if (behavior.browsedMerchantIds.stream().map(index::get).filter(Objects::nonNull)
                .anyMatch(m -> Objects.equals(m.getCategory(), merchant.getCategory()))) {
            return "根据你近期浏览的同类商家推荐";
        }
        return "口碑与热度优选";
    }

    private void addCuisineWeight(Map<String, Double> weights, Merchant merchant, double weight) {
        if (merchant != null && StringUtils.hasText(merchant.getCategory())) {
            weights.merge(merchant.getCategory(), weight, Double::sum);
        }
    }

    private void normalizeScores(List<Merchant> merchants) {
        double min = merchants.stream().map(Merchant::getRecommendScore).min(Double::compare).orElse(0.0);
        double max = merchants.stream().map(Merchant::getRecommendScore).max(Double::compare).orElse(0.0);
        for (Merchant merchant : merchants) {
            double raw = merchant.getRecommendScore() == null ? 0 : merchant.getRecommendScore();
            double normalized = max - min < 0.0001 ? 80.0 : 10 + (raw - min) / (max - min) * 89;
            merchant.setRecommendScore(round(normalized));
        }
    }

    private double baseScore(Merchant merchant) {
        double score = merchant.getScore() == null ? 0 : merchant.getScore().doubleValue();
        return score * 12
                + Math.log1p(nullToZero(merchant.getFavoriteCount())) * 4
                + Math.log1p(nullToZero(merchant.getReviewCount())) * 3
                + Math.log1p(nullToZero(merchant.getViewCount())) * 1.5;
    }

    private double heatOf(Merchant merchant) {
        double score = merchant.getScore() == null ? 0 : merchant.getScore().doubleValue();
        return nullToZero(merchant.getFavoriteCount()) * 3.0
                + nullToZero(merchant.getReviewCount()) * 2.0
                + nullToZero(merchant.getViewCount()) * 0.2
                + score * 10;
    }

    private void writeLogs(Long userId, List<Merchant> merchants, String algorithm) {
        if (merchants.isEmpty()) {
            return;
        }
        String requestId = "R" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        for (Merchant merchant : merchants) {
            RecommendLog logEntry = new RecommendLog();
            logEntry.setUserId(userId);
            logEntry.setMerchantId(merchant.getId());
            logEntry.setAlgorithm(algorithm);
            logEntry.setScore(merchant.getRecommendScore());
            logEntry.setReason(merchant.getRecommendReason());
            logEntry.setRequestId(requestId);
            try {
                recommendLogMapper.insert(logEntry);
            } catch (Exception e) {
                // 日志失败不影响主流程
                log.debug("推荐日志写入失败: {}", e.getMessage());
            }
        }
    }

    private Set<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return new HashSet<>();
        }
        return Arrays.stream(value.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 10;
        }
        return Math.min(limit, 50);
    }

    private double nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double priceDiff(Merchant a, Merchant b) {
        BigDecimal pa = a.getAvgPrice() == null ? BigDecimal.ZERO : a.getAvgPrice();
        BigDecimal pb = b.getAvgPrice() == null ? BigDecimal.ZERO : b.getAvgPrice();
        return Math.abs(pa.subtract(pb).doubleValue());
    }

    private Double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /**
     * 用户行为数据。
     */
    private static class UserBehavior {
        private Set<Long> favoriteIds = new HashSet<>();
        private Set<Long> reviewedMerchantIds = new HashSet<>();
        private Set<Long> browsedMerchantIds = new HashSet<>();
        private Set<Long> likedMerchantIds = new HashSet<>();
        private Set<Long> dislikedMerchantIds = new HashSet<>();
    }
}
