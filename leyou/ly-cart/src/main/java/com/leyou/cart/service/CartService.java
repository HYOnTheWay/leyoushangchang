package com.leyou.cart.service;

import com.leyou.cart.interceptor.UserInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:uid:";

    @Transactional
    public void addCart(Cart cart) {
        //获取登录用户
        UserInfo user = UserInterceptor.getUser();
        //key
        String key = KEY_PREFIX + user.getId();
        String hashKey = cart.getSkuId().toString();
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(key);
        //判断当前购物车东西是否存在
        if (operation.hasKey(hashKey)) {
            //是,修改数量
            String cartJson = operation.get(hashKey).toString();
            Cart cacheCart = JsonUtils.parse(cartJson, Cart.class);
            cacheCart.setNum(cacheCart.getNum() + cart.getNum());
            //写回redis
            operation.put(hashKey,JsonUtils.serialize(cacheCart));
        }else {
            //否,新增
            operation.put(hashKey,JsonUtils.serialize(cart));
        }
    }

    public List<Cart> queryCartList() {
        //获取登录用户
        UserInfo user = UserInterceptor.getUser();
        //key
        String key = KEY_PREFIX + user.getId();
        if (!redisTemplate.hasKey(key)){
            //key不存在,返回404
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        //获取登陆的所有购物车
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(key);
        List<Cart> cartList = operation.values().stream().map(
                o -> JsonUtils.parse(o.toString(), Cart.class)).collect(Collectors.toList());
        return cartList;
    }

    public void updateCartNum(Long skuId, Integer num) {
        //获取登录用户
        UserInfo user = UserInterceptor.getUser();
        //key
        String key = KEY_PREFIX + user.getId();
        //获取操作
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        //判断key是否存在
        if (!operations.hasKey(skuId.toString())) {
            //key不存在,返回404
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        //查询
        Cart cart = JsonUtils.parse(operations.get(skuId.toString()).toString(), Cart.class);
        cart.setNum(num);
        //写回redis
        operations.put(skuId.toString(),JsonUtils.serialize(cart));
    }

    public void deleteCart(Long skuId) {
        //获取登录用户
        UserInfo user = UserInterceptor.getUser();
        //key
        String key = KEY_PREFIX + user.getId();
        //删除
        redisTemplate.opsForHash().delete(key,skuId.toString());
    }
}
