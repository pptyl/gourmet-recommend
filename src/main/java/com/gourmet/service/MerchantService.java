package com.gourmet.service;

import com.gourmet.common.result.PageResult;
import com.gourmet.dto.LoginDTO;
import com.gourmet.dto.MerchantApplyDTO;
import com.gourmet.dto.MerchantQuery;
import com.gourmet.dto.MerchantUpdateDTO;
import com.gourmet.entity.Merchant;
import com.gourmet.vo.LoginVO;

import java.util.List;

/**
 * 商家入驻与店铺信息。
 */
public interface MerchantService {

    /** 提交入驻申请 */
    Merchant apply(MerchantApplyDTO dto);

    /** 商家登录 */
    LoginVO login(LoginDTO dto);

    /** 查看本店资料 */
    Merchant getProfile(Long merchantId);

    /** 修改店铺资料 */
    Merchant updateProfile(Long merchantId, MerchantUpdateDTO dto);

    /** 前台分页浏览商家（按地区 / 菜系 / 评分筛选） */
    PageResult<Merchant> page(MerchantQuery query);

    /** 商家详情（累加浏览量、记录浏览历史） */
    Merchant detail(Long merchantId);

    /** 内部使用：查询已通过审核的商家 */
    Merchant getByIdOrThrow(Long merchantId);

    /** 内部使用：全部审核通过且在营业的商家 */
    List<Merchant> listApproved();
}
