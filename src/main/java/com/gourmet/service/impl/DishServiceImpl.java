package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.dto.DishDTO;
import com.gourmet.dto.DishQuery;
import com.gourmet.entity.Dish;
import com.gourmet.entity.Favorite;
import com.gourmet.entity.Merchant;
import com.gourmet.mapper.DishMapper;
import com.gourmet.mapper.FavoriteMapper;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.service.DishService;
import com.gourmet.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 菜品服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;
    private final MerchantMapper merchantMapper;
    private final FavoriteMapper favoriteMapper;
    private final MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Dish create(Long merchantId, DishDTO dto) {
        Dish dish = new Dish();
        dish.setMerchantId(merchantId);
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        dish.setIntro(dto.getIntro());
        dish.setImage(dto.getImage());
        dish.setStatus(BizConstants.DISH_ON_SALE);
        dish.setSales(0);
        dishMapper.insert(dish);

        // 新品上架通知：推送给收藏本店的用户
        notifyFavoriteUsers(merchantId, dish);
        return dish;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Dish update(Long merchantId, Long dishId, DishDTO dto) {
        Dish dish = getOwnedDish(merchantId, dishId);
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        dish.setIntro(dto.getIntro());
        dish.setImage(dto.getImage());
        dishMapper.updateById(dish);
        return dish;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long merchantId, Long dishId, Integer status) {
        if (status == null || (status != BizConstants.DISH_ON_SALE && status != BizConstants.DISH_OFF_SALE)) {
            throw new BusinessException("菜品状态只能为 0 或 1");
        }
        Dish dish = getOwnedDish(merchantId, dishId);
        boolean relist = status == BizConstants.DISH_ON_SALE
                && dish.getStatus() != null && dish.getStatus() == BizConstants.DISH_OFF_SALE;
        dish.setStatus(status);
        dishMapper.updateById(dish);
        if (relist) {
            notifyFavoriteUsers(merchantId, dish);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long merchantId, Long dishId) {
        getOwnedDish(merchantId, dishId);
        dishMapper.deleteById(dishId);
    }

    @Override
    public Dish detail(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜品不存在");
        }
        Merchant merchant = merchantMapper.selectById(dish.getMerchantId());
        if (merchant != null) {
            dish.setMerchantName(merchant.getName());
        }
        return dish;
    }

    @Override
    public PageResult<Dish> page(DishQuery query) {
        Page<Dish> page = query.toPage();
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(query.getMerchantId() != null, Dish::getMerchantId, query.getMerchantId())
                .eq(query.getStatus() != null, Dish::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Dish::getName, query.getKeyword())
                .orderByDesc(Dish::getStatus)
                .orderByDesc(Dish::getSales);
        Page<Dish> result = dishMapper.selectPage(page, wrapper);

        // 填充店铺名称
        if (!result.getRecords().isEmpty()) {
            List<Long> merchantIds = result.getRecords().stream()
                    .map(Dish::getMerchantId).distinct().toList();
            List<Merchant> merchants = merchantMapper.selectBatchIds(merchantIds);
            result.getRecords().forEach(dish -> merchants.stream()
                    .filter(m -> m.getId().equals(dish.getMerchantId()))
                    .findFirst()
                    .ifPresent(m -> dish.setMerchantName(m.getName())));
        }
        return PageResult.of(result);
    }

    @Override
    public List<Dish> listByMerchant(Long merchantId) {
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getMerchantId, merchantId)
                .eq(Dish::getStatus, BizConstants.DISH_ON_SALE)
                .orderByDesc(Dish::getSales));
    }

    @Override
    public long countByMerchant(Long merchantId) {
        Long count = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getMerchantId, merchantId));
        return count == null ? 0 : count;
    }

    private Dish getOwnedDish(Long merchantId, Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜品不存在");
        }
        if (!dish.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作本店菜品");
        }
        return dish;
    }

    /**
     * 新品上架消息推送给收藏本店的用户（最多 200 人，避免一次写入过多）。
     */
    private void notifyFavoriteUsers(Long merchantId, Dish dish) {
        try {
            List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getMerchantId, merchantId)
                    .last("limit 200"));
            if (favorites.isEmpty()) {
                return;
            }
            Merchant merchant = merchantMapper.selectById(merchantId);
            String shopName = merchant == null ? "你收藏的店铺" : merchant.getName();
            favorites.forEach(favorite -> messageService.push(
                    favorite.getUserId(),
                    BizConstants.RECEIVER_USER,
                    BizConstants.MSG_NEW_DISH,
                    "商家上新：" + dish.getName(),
                    "「" + shopName + "」上架了新菜品「" + dish.getName() + "」，售价 " + dish.getPrice() + " 元，欢迎品尝。",
                    dish.getId()));
        } catch (Exception e) {
            log.warn("新品上架消息推送失败：{}", e.getMessage());
        }
    }
}
