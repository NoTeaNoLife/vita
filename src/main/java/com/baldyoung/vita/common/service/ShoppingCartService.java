package com.baldyoung.vita.common.service;

import com.baldyoung.vita.common.pojo.entity.ShoppingCartItem;

import java.util.List;
import java.util.Map;

public interface ShoppingCartService {
    Integer p = 1;

    static void init() {

    }

    default void test(){

    }

    /**
     * 清空购物车
     * @param shoppingCartId
     */
    void clearShoppingCart(Integer shoppingCartId);

    /**
     * 修改购物车商品
     * @param shoppingCartId
     * @param itemList
     */
    void setProductForShoppingCart(Integer shoppingCartId, Map<Integer, Integer> itemData);

    /**
     * 获取购物车中所有的商品
     * @param shoppingCartId
     * @return
     */
    Map<Integer, Integer> getAllProductFromShoppingCar(Integer shoppingCartId);

    /**
     * 整理购物车中的商品
     * @param shoppingCartId
     * @return
     */
    List<ShoppingCartItem> packageData(Integer shoppingCartId);








}
