package com.gourmet.service;

import com.gourmet.entity.Merchant;
import com.gourmet.vo.AdminOverviewVO;
import com.gourmet.vo.CouponStatVO;
import com.gourmet.vo.MerchantStatisticVO;
import com.gourmet.vo.UserStatisticVO;

import java.util.List;

/**
 * 数据统计。
 */
public interface StatisticService {

    /** 个人探店数据 */
    UserStatisticVO userStatistic(Long userId);

    /** 商家店铺数据 */
    MerchantStatisticVO merchantStatistic(Long merchantId);

    /** 平台整体业务数据 */
    AdminOverviewVO adminOverview();

    /** 店铺热度排行 */
    List<Merchant> merchantHeatRank(int limit);

    /** 优惠券领取数据（merchantId 为空则统计全平台） */
    List<CouponStatVO> couponStatistics(Long merchantId);
}
