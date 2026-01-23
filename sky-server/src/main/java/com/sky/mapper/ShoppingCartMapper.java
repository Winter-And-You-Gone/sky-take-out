package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 动态条件查询
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据id修改商品数量
     * @param shoppingCart
     * @return
     */
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 插入数据
     * @param shoppingCart
     * @return
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户id清空购物车
     * @param userId
     * @return
     */
    void deleteByUserId(Long userId);

    /**
     * 删除购物车中一个商品
     * @param shoppingCart
     * @return
     */
    void deleteOne(ShoppingCart shoppingCart);
}
