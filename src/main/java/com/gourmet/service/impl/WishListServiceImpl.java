package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.dto.WishListDTO;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.WishList;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.WishListMapper;
import com.gourmet.service.WishListService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 心愿单服务实现。
 */
@Service
@RequiredArgsConstructor
public class WishListServiceImpl implements WishListService {

    private final WishListMapper wishListMapper;
    private final MerchantMapper merchantMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WishList add(Long userId, WishListDTO dto) {
        Merchant merchant = merchantMapper.selectById(dto.getMerchantId());
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        }
        Long exists = wishListMapper.selectCount(new LambdaQueryWrapper<WishList>()
                .eq(WishList::getUserId, userId)
                .eq(WishList::getMerchantId, dto.getMerchantId()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该店铺已在心愿单中");
        }

        WishList wishList = new WishList();
        wishList.setUserId(userId);
        wishList.setMerchantId(dto.getMerchantId());
        wishList.setRemark(dto.getRemark());
        wishList.setPlanDate(dto.getPlanDate());
        try {
            wishListMapper.insert(wishList);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "该店铺已在心愿单中");
        }
        return wishList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WishList update(Long userId, Long id, WishListDTO dto) {
        WishList wishList = getOwned(userId, id);
        wishList.setRemark(dto.getRemark());
        wishList.setPlanDate(dto.getPlanDate());
        wishListMapper.updateById(wishList);
        return wishList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwned(userId, id);
        wishListMapper.deleteById(id);
    }

    @Override
    public PageResult<WishList> page(Long userId, PageQuery query) {
        Page<WishList> page = query.toPage();
        Page<WishList> result = wishListMapper.selectPage(page, new LambdaQueryWrapper<WishList>()
                .eq(WishList::getUserId, userId)
                .orderByDesc(WishList::getCreateTime));
        fillMerchantFields(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public long count(Long userId) {
        Long count = wishListMapper.selectCount(new LambdaQueryWrapper<WishList>()
                .eq(WishList::getUserId, userId));
        return count == null ? 0 : count;
    }

    private WishList getOwned(Long userId, Long id) {
        WishList wishList = wishListMapper.selectById(id);
        if (wishList == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "心愿单条目不存在");
        }
        if (!wishList.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己的心愿单");
        }
        return wishList;
    }

    private void fillMerchantFields(List<WishList> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> merchantIds = records.stream().map(WishList::getMerchantId).distinct().toList();
        Map<Long, Merchant> merchantMap = merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));
        for (WishList wish : records) {
            Merchant merchant = merchantMap.get(wish.getMerchantId());
            if (merchant != null) {
                wish.setMerchantName(merchant.getName());
                wish.setCoverImage(merchant.getCoverImage());
                wish.setCategory(merchant.getCategory());
                wish.setAddress(merchant.getAddress());
                wish.setScore(merchant.getScore());
            }
        }
    }
}
