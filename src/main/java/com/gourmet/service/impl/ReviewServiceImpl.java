package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.dto.ReviewCreateDTO;
import com.gourmet.dto.ReviewQuery;
import com.gourmet.entity.Dish;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Review;
import com.gourmet.entity.ReviewLike;
import com.gourmet.entity.User;
import com.gourmet.mapper.DishMapper;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.ReviewLikeMapper;
import com.gourmet.mapper.ReviewMapper;
import com.gourmet.mapper.UserMapper;
import com.gourmet.service.MessageService;
import com.gourmet.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评价服务实现：发布、查询、审核与商家评分聚合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final DishMapper dishMapper;
    private final ReviewLikeMapper reviewLikeMapper;
    private final MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Review create(Long userId, ReviewCreateDTO dto) {
        Merchant merchant = merchantMapper.selectById(dto.getMerchantId());
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        }
        if (merchant.getAuditStatus() == null || merchant.getAuditStatus() != BizConstants.MERCHANT_AUDIT_PASSED) {
            throw new BusinessException("该商家尚未通过平台审核，暂不支持评价");
        }
        if (dto.getDishId() != null) {
            Dish dish = dishMapper.selectById(dto.getDishId());
            if (dish == null || !dish.getMerchantId().equals(dto.getMerchantId())) {
                throw new BusinessException("菜品与商家不匹配");
            }
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setMerchantId(dto.getMerchantId());
        review.setDishId(dto.getDishId());
        review.setScore(dto.getScore());
        review.setContent(dto.getContent());
        review.setImages(dto.getImages() == null || dto.getImages().isEmpty()
                ? null : String.join(",", dto.getImages()));
        review.setStatus(BizConstants.CONTENT_PENDING);
        review.setLikeCount(0);
        reviewMapper.insert(review);

        log.info("用户 {} 发布评价 {}，等待审核", userId, review.getId());
        return review;
    }

    @Override
    public PageResult<Review> pageMine(Long userId, ReviewQuery query) {
        Page<Review> page = query.toPage();
        Page<Review> result = reviewMapper.selectPage(page, new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .orderByDesc(Review::getCreateTime));
        fillMerchantNames(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public PageResult<Review> pageByMerchant(Long merchantId, ReviewQuery query) {
        Page<Review> page = query.toPage();
        Page<Review> result = reviewMapper.selectPage(page, new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchantId)
                .eq(Review::getStatus, BizConstants.CONTENT_APPROVED)
                .orderByDesc(Review::getCreateTime));
        fillUserInfo(result.getRecords());
        fillMerchantNames(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public PageResult<Review> pageAll(ReviewQuery query) {
        Page<Review> page = query.toPage();
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(query.getMerchantId() != null, Review::getMerchantId, query.getMerchantId())
                .eq(query.getStatus() != null, Review::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Review::getContent, query.getKeyword())
                .orderByDesc(Review::getCreateTime);
        Page<Review> result = reviewMapper.selectPage(page, wrapper);
        fillUserInfo(result.getRecords());
        fillMerchantNames(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public Review detail(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        fillUserInfo(List.of(review));
        fillMerchantNames(List.of(review));
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMine(Long userId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的评价");
        }
        reviewMapper.deleteById(reviewId);
        refreshMerchantScore(review.getMerchantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long reviewId, boolean pass, String remark) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        review.setStatus(pass ? BizConstants.CONTENT_APPROVED : BizConstants.CONTENT_REJECTED);
        reviewMapper.updateById(review);
        refreshMerchantScore(review.getMerchantId());

        messageService.push(review.getUserId(), BizConstants.RECEIVER_USER, BizConstants.MSG_AUDIT,
                pass ? "评价审核通过" : "评价未通过审核",
                pass ? "你发布的评价已通过审核，感谢分享美食体验！"
                        : "你发布的评价未通过审核" + (StringUtils.hasText(remark) ? "：" + remark : "，请遵守社区规范。"),
                review.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshMerchantScore(Long merchantId) {
        Double avg = reviewMapper.selectAvgScore(merchantId);
        Long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchantId)
                .eq(Review::getStatus, BizConstants.CONTENT_APPROVED));
        int reviewCount = count == null ? 0 : count.intValue();
        BigDecimal score = avg == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

        merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, merchantId)
                .set(Merchant::getScore, score)
                .set(Merchant::getScoreCount, reviewCount)
                .set(Merchant::getReviewCount, reviewCount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int like(Long userId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        Long exists = reviewLikeMapper.selectCount(new LambdaQueryWrapper<ReviewLike>()
                .eq(ReviewLike::getUserId, userId)
                .eq(ReviewLike::getReviewId, reviewId));
        if (exists == null || exists == 0) {
            ReviewLike like = new ReviewLike();
            like.setUserId(userId);
            like.setReviewId(reviewId);
            try {
                reviewLikeMapper.insert(like);
                reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                        .eq(Review::getId, reviewId)
                        .setSql("like_count = like_count + 1"));
            } catch (DuplicateKeyException e) {
                log.debug("重复点赞已忽略：user={}, review={}", userId, reviewId);
            }
        }
        Review latest = reviewMapper.selectById(reviewId);
        return latest == null || latest.getLikeCount() == null ? 0 : latest.getLikeCount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unlike(Long userId, Long reviewId) {
        int deleted = reviewLikeMapper.delete(new LambdaQueryWrapper<ReviewLike>()
                .eq(ReviewLike::getUserId, userId)
                .eq(ReviewLike::getReviewId, reviewId));
        if (deleted > 0) {
            reviewMapper.update(null, new LambdaUpdateWrapper<Review>()
                    .eq(Review::getId, reviewId)
                    .setSql("like_count = case when like_count > 0 then like_count - 1 else 0 end"));
        }
        Review latest = reviewMapper.selectById(reviewId);
        return latest == null || latest.getLikeCount() == null ? 0 : latest.getLikeCount();
    }

    @Override
    public boolean isLiked(Long userId, Long reviewId) {
        if (userId == null || reviewId == null) {
            return false;
        }
        Long count = reviewLikeMapper.selectCount(new LambdaQueryWrapper<ReviewLike>()
                .eq(ReviewLike::getUserId, userId)
                .eq(ReviewLike::getReviewId, reviewId));
        return count != null && count > 0;
    }

    @Override
    public int likeCount(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        return review == null || review.getLikeCount() == null ? 0 : review.getLikeCount();
    }

    private void fillUserInfo(List<Review> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(Review::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        for (Review review : records) {
            User user = userMap.get(review.getUserId());
            if (user != null) {
                review.setUserName(user.getNickname());
                review.setUserAvatar(user.getAvatar());
            }
        }
    }

    private void fillMerchantNames(List<Review> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> merchantIds = records.stream().map(Review::getMerchantId).distinct().toList();
        Map<Long, Merchant> merchantMap = merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));
        for (Review review : records) {
            Merchant merchant = merchantMap.get(review.getMerchantId());
            if (merchant != null) {
                review.setMerchantName(merchant.getName());
            }
            review.setLiked(false);
        }

        // 当前登录用户的点赞状态
        Long viewer = UserContext.getUserIdOrNull();
        if (viewer != null) {
            List<Long> reviewIds = records.stream().map(Review::getId).toList();
            Set<Long> likedIds = reviewLikeMapper.selectList(new LambdaQueryWrapper<ReviewLike>()
                            .eq(ReviewLike::getUserId, viewer)
                            .in(ReviewLike::getReviewId, reviewIds))
                    .stream().map(ReviewLike::getReviewId).collect(Collectors.toSet());
            records.forEach(review -> review.setLiked(likedIds.contains(review.getId())));
        }
    }
}
