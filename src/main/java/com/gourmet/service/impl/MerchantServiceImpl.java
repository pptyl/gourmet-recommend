package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.model.LoginUser;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.common.util.JwtUtil;
import com.gourmet.dto.LoginDTO;
import com.gourmet.dto.MerchantApplyDTO;
import com.gourmet.dto.MerchantQuery;
import com.gourmet.dto.MerchantUpdateDTO;
import com.gourmet.entity.BrowseHistory;
import com.gourmet.entity.Favorite;
import com.gourmet.entity.Merchant;
import com.gourmet.mapper.BrowseHistoryMapper;
import com.gourmet.mapper.FavoriteMapper;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.service.MerchantService;
import com.gourmet.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商家服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final FavoriteMapper favoriteMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Merchant apply(MerchantApplyDTO dto) {
        Long exists = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getPhone, dto.getPhone()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该手机号已提交过入驻申请");
        }

        Merchant merchant = new Merchant();
        merchant.setName(dto.getName());
        merchant.setPhone(dto.getPhone());
        merchant.setPassword(passwordEncoder.encode(dto.getPassword()));
        merchant.setCategory(dto.getCategory());
        merchant.setArea(dto.getArea());
        merchant.setAddress(dto.getAddress());
        merchant.setIntro(dto.getIntro());
        merchant.setBusinessHours(dto.getBusinessHours());
        merchant.setAvgPrice(dto.getAvgPrice() == null ? BigDecimal.ZERO : dto.getAvgPrice());
        merchant.setScore(BigDecimal.ZERO);
        merchant.setScoreCount(0);
        merchant.setFavoriteCount(0);
        merchant.setReviewCount(0);
        merchant.setViewCount(0);
        merchant.setStatus(BizConstants.MERCHANT_OPEN);
        merchant.setAuditStatus(BizConstants.MERCHANT_AUDIT_PENDING);
        merchantMapper.insert(merchant);

        log.info("收到商家入驻申请：id={}, name={}", merchant.getId(), merchant.getName());
        return merchant;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getPhone, dto.getPhone()));
        if (merchant == null || !passwordEncoder.matches(dto.getPassword(), merchant.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "手机号或密码错误");
        }
        if (merchant.getAuditStatus() != null && merchant.getAuditStatus() == BizConstants.MERCHANT_AUDIT_PENDING) {
            throw new BusinessException(ResultCode.FORBIDDEN, "入驻申请正在审核中，请耐心等待");
        }
        if (merchant.getAuditStatus() != null && merchant.getAuditStatus() == BizConstants.MERCHANT_AUDIT_REJECTED) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "入驻申请已被驳回" + (StringUtils.hasText(merchant.getAuditRemark()) ? "：" + merchant.getAuditRemark() : ""));
        }
        if (merchant.getStatus() != null && merchant.getStatus() == BizConstants.MERCHANT_CLOSED) {
            throw new BusinessException(ResultCode.FORBIDDEN, "店铺处于停业整顿状态，暂不可登录");
        }

        LoginUser loginUser = new LoginUser(merchant.getId(), BizConstants.ROLE_MERCHANT, merchant.getName());
        String token = jwtUtil.createToken(loginUser);
        return LoginVO.of(token, BizConstants.ROLE_MERCHANT, merchant.getId(),
                merchant.getName(), merchant.getCoverImage(), jwtUtil.getExpireMinutes());
    }

    @Override
    public Merchant getProfile(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "店铺不存在");
        }
        return merchant;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Merchant updateProfile(Long merchantId, MerchantUpdateDTO dto) {
        Merchant merchant = getProfile(merchantId);
        merchant.setName(dto.getName());
        merchant.setCategory(dto.getCategory());
        merchant.setArea(dto.getArea());
        merchant.setAddress(dto.getAddress());
        merchant.setIntro(dto.getIntro());
        merchant.setCoverImage(dto.getCoverImage());
        merchant.setBusinessHours(dto.getBusinessHours());
        merchant.setAvgPrice(dto.getAvgPrice());
        if (dto.getAlbum() != null) {
            merchant.setAlbum(String.join(",", dto.getAlbum()));
        }
        merchantMapper.updateById(merchant);
        return merchant;
    }

    @Override
    public PageResult<Merchant> page(MerchantQuery query) {
        Page<Merchant> page = query.toPage();
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getAuditStatus, BizConstants.MERCHANT_AUDIT_PASSED)
                .eq(Merchant::getStatus, BizConstants.MERCHANT_OPEN);

        if (StringUtils.hasText(query.getArea())) {
            wrapper.like(Merchant::getArea, query.getArea());
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(Merchant::getCategory, query.getCategory());
        }
        if (query.getMinScore() != null) {
            wrapper.ge(Merchant::getScore, query.getMinScore());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Merchant::getName, query.getKeyword())
                    .or().like(Merchant::getIntro, query.getKeyword())
                    .or().like(Merchant::getAddress, query.getKeyword()));
        }

        String sort = query.getSort();
        if ("favorite".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Merchant::getFavoriteCount);
        } else if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Merchant::getViewCount);
        } else if ("new".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Merchant::getCreateTime);
        } else {
            wrapper.orderByDesc(Merchant::getScore).orderByDesc(Merchant::getFavoriteCount);
        }
        return PageResult.of(merchantMapper.selectPage(page, wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Merchant detail(Long merchantId) {
        Merchant merchant = getByIdOrThrow(merchantId);

        // 浏览量 +1（热度来源之一）
        merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, merchantId)
                .setSql("view_count = view_count + 1"));
        merchant.setViewCount((merchant.getViewCount() == null ? 0 : merchant.getViewCount()) + 1);

        Long userId = UserContext.getUserIdOrNull();
        if (userId != null) {
            // 记录浏览历史，作为推荐依据
            BrowseHistory history = new BrowseHistory();
            history.setUserId(userId);
            history.setMerchantId(merchantId);
            browseHistoryMapper.insert(history);

            Long favoriteCount = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getMerchantId, merchantId));
            merchant.setFavorite(favoriteCount != null && favoriteCount > 0);
        }
        return merchant;
    }

    @Override
    public Merchant getByIdOrThrow(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        }
        if (merchant.getAuditStatus() == null || merchant.getAuditStatus() != BizConstants.MERCHANT_AUDIT_PASSED) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该商家尚未通过平台审核");
        }
        return merchant;
    }

    @Override
    public List<Merchant> listApproved() {
        return merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getAuditStatus, BizConstants.MERCHANT_AUDIT_PASSED)
                .eq(Merchant::getStatus, BizConstants.MERCHANT_OPEN));
    }
}
