package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.entity.BrowseHistory;
import com.gourmet.entity.Coupon;
import com.gourmet.entity.Favorite;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Note;
import com.gourmet.entity.Review;
import com.gourmet.entity.User;
import com.gourmet.entity.UserCoupon;
import com.gourmet.entity.WishList;
import com.gourmet.mapper.BrowseHistoryMapper;
import com.gourmet.mapper.CouponMapper;
import com.gourmet.mapper.DishMapper;
import com.gourmet.mapper.FavoriteMapper;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.MessageMapper;
import com.gourmet.mapper.NoteMapper;
import com.gourmet.mapper.ReviewMapper;
import com.gourmet.mapper.UserCouponMapper;
import com.gourmet.mapper.UserMapper;
import com.gourmet.mapper.WishListMapper;
import com.gourmet.service.RecommendService;
import com.gourmet.service.StatisticService;
import com.gourmet.vo.AdminOverviewVO;
import com.gourmet.vo.CouponStatVO;
import com.gourmet.vo.MerchantStatisticVO;
import com.gourmet.vo.UserStatisticVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现。
 */
@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final ReviewMapper reviewMapper;
    private final NoteMapper noteMapper;
    private final FavoriteMapper favoriteMapper;
    private final WishListMapper wishListMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final MessageMapper messageMapper;
    private final DishMapper dishMapper;
    private final RecommendService recommendService;

    @Override
    public UserStatisticVO userStatistic(Long userId) {
        UserStatisticVO vo = new UserStatisticVO();
        vo.setReviewCount(count(reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId))));
        vo.setNoteCount(count(noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId))));
        vo.setFavoriteCount(count(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId))));
        vo.setWishCount(count(wishListMapper.selectCount(new LambdaQueryWrapper<WishList>()
                .eq(WishList::getUserId, userId))));
        vo.setCouponCount(count(userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId))));
        vo.setUnusedCouponCount(count(userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, BizConstants.USER_COUPON_UNUSED))));
        vo.setBrowseCount(browseHistoryMapper.selectList(new LambdaQueryWrapper<BrowseHistory>()
                        .eq(BrowseHistory::getUserId, userId))
                .stream().map(BrowseHistory::getMerchantId).distinct().count());
        vo.setUnreadMessageCount(messageMapper.selectCount(new LambdaQueryWrapper<com.gourmet.entity.Message>()
                .eq(com.gourmet.entity.Message::getReceiverId, userId)
                .eq(com.gourmet.entity.Message::getReceiverType, BizConstants.RECEIVER_USER)
                .eq(com.gourmet.entity.Message::getIsRead, 0)));
        return vo;
    }

    @Override
    public MerchantStatisticVO merchantStatistic(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new com.gourmet.common.exception.BusinessException(
                    com.gourmet.common.result.ResultCode.NOT_FOUND, "店铺不存在");
        }
        MerchantStatisticVO vo = new MerchantStatisticVO();
        vo.setMerchantId(merchant.getId());
        vo.setName(merchant.getName());
        vo.setScore(merchant.getScore());
        vo.setFavoriteCount(count(merchant.getFavoriteCount() == null ? 0 : merchant.getFavoriteCount().longValue()));
        vo.setViewCount(count(merchant.getViewCount() == null ? 0 : merchant.getViewCount().longValue()));
        vo.setReviewCount(count(reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchantId)
                .eq(Review::getStatus, BizConstants.CONTENT_APPROVED))));
        vo.setDishCount(count(dishMapper.selectCount(new LambdaQueryWrapper<com.gourmet.entity.Dish>()
                .eq(com.gourmet.entity.Dish::getMerchantId, merchantId))));
        vo.setCouponCount(count(couponMapper.selectCount(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getMerchantId, merchantId))));
        vo.setCouponClaimCount(userCouponMapper.countByMerchant(merchantId));
        return vo;
    }

    @Override
    public AdminOverviewVO adminOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        AdminOverviewVO vo = new AdminOverviewVO();
        vo.setUserCount(count(userMapper.selectCount(null)));
        vo.setMerchantCount(count(merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getAuditStatus, BizConstants.MERCHANT_AUDIT_PASSED))));
        vo.setPendingMerchantCount(count(merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getAuditStatus, BizConstants.MERCHANT_AUDIT_PENDING))));
        vo.setReviewCount(count(reviewMapper.selectCount(null)));
        vo.setPendingReviewCount(count(reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getStatus, BizConstants.CONTENT_PENDING))));
        vo.setNoteCount(count(noteMapper.selectCount(null)));
        vo.setPendingNoteCount(count(noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getStatus, BizConstants.CONTENT_PENDING))));
        vo.setCouponCount(count(couponMapper.selectCount(null)));
        vo.setCouponClaimCount(count(userCouponMapper.selectCount(null)));
        vo.setTodayUserCount(count(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, todayStart))));
        vo.setTodayReviewCount(count(reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .ge(Review::getCreateTime, todayStart))));
        return vo;
    }

    @Override
    public List<Merchant> merchantHeatRank(int limit) {
        return recommendService.hotRank(limit);
    }

    @Override
    public List<CouponStatVO> couponStatistics(Long merchantId) {
        return couponMapper.selectCouponStats(merchantId);
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
