package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.entity.Favorite;
import com.gourmet.entity.Merchant;
import com.gourmet.mapper.FavoriteMapper;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏服务实现。
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final MerchantMapper merchantMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        }
        Long exists = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getMerchantId, merchantId));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "已经收藏过该店铺了");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setMerchantId(merchantId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "已经收藏过该店铺了");
        }

        merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, merchantId)
                .setSql("favorite_count = favorite_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long merchantId) {
        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getMerchantId, merchantId));
        if (deleted == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "尚未收藏该店铺");
        }
        merchantMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, merchantId)
                .setSql("favorite_count = case when favorite_count > 0 then favorite_count - 1 else 0 end"));
    }

    @Override
    public PageResult<Favorite> page(Long userId, PageQuery query) {
        Page<Favorite> page = query.toPage();
        Page<Favorite> result = favoriteMapper.selectPage(page, new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime));
        fillMerchantFields(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public boolean check(Long userId, Long merchantId) {
        if (userId == null || merchantId == null) {
            return false;
        }
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getMerchantId, merchantId));
        return count != null && count > 0;
    }

    @Override
    public long count(Long userId) {
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId));
        return count == null ? 0 : count;
    }

    @Override
    public List<Long> listMerchantIds(Long userId) {
        return favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId))
                .stream()
                .map(Favorite::getMerchantId)
                .toList();
    }

    private void fillMerchantFields(List<Favorite> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> merchantIds = records.stream().map(Favorite::getMerchantId).distinct().toList();
        Map<Long, Merchant> merchantMap = merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));
        for (Favorite favorite : records) {
            Merchant merchant = merchantMap.get(favorite.getMerchantId());
            if (merchant != null) {
                favorite.setMerchantName(merchant.getName());
                favorite.setCoverImage(merchant.getCoverImage());
                favorite.setCategory(merchant.getCategory());
                favorite.setArea(merchant.getArea());
                favorite.setAddress(merchant.getAddress());
                favorite.setScore(merchant.getScore());
                favorite.setAvgPrice(merchant.getAvgPrice());
            }
        }
    }
}
