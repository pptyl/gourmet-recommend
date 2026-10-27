package com.gourmet.service;

import com.gourmet.common.result.PageResult;
import com.gourmet.dto.DishDTO;
import com.gourmet.dto.DishQuery;
import com.gourmet.entity.Dish;

import java.util.List;

/**
 * 菜品管理。
 */
public interface DishService {

    /** 新增菜品 */
    Dish create(Long merchantId, DishDTO dto);

    /** 编辑菜品 */
    Dish update(Long merchantId, Long dishId, DishDTO dto);

    /** 上架 / 下架 */
    void updateStatus(Long merchantId, Long dishId, Integer status);

    /** 删除菜品（逻辑删除） */
    void delete(Long merchantId, Long dishId);

    /** 菜品详情 */
    Dish detail(Long dishId);

    /** 分页查询菜品 */
    PageResult<Dish> page(DishQuery query);

    /** 某商家全部在售菜品 */
    List<Dish> listByMerchant(Long merchantId);

    /** 内部使用：菜品数量统计 */
    long countByMerchant(Long merchantId);
}
