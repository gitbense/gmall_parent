package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zr
 * @create 2020-03-27 上午 11:10
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    // 添加购物车
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /*
        根据skuId和userId从数据库中查询数据
        不为空：说明购物车中存在该商品，直接加数量修改即可
        为空：购物车中没有该商品，需insert
        最后将cartInfo加入缓存
         */
        // 获取购物车的key
        String cartKey = getCartKey(userId);
        if (!redisTemplate.hasKey(cartKey)) {
            // 加载数据库的数据到缓存
            loadCartCache(userId);
        }
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("sku_id", skuId).eq("user_id", userId);
        CartInfo cartInfo = cartInfoMapper.selectOne(cartInfoQueryWrapper);
        if (cartInfo != null) {
            // 购物车中存在该商品，直接加数量修改即可
            cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
            // 查询最新价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfo.setSkuPrice(skuPrice);
            cartInfoMapper.updateById(cartInfo);
        } else {
            // 购物车中没有该商品，需insert
            CartInfo info = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            info.setSkuId(skuId);
            info.setUserId(userId);
            info.setSkuNum(skuNum);
            info.setCartPrice(skuInfo.getPrice());
            info.setSkuPrice(skuInfo.getPrice());
            info.setImgUrl(skuInfo.getSkuDefaultImg());
            info.setSkuName(skuInfo.getSkuName());

            // 添加到数据库
            cartInfoMapper.insert(info);
            cartInfo = info;
        }
        // 更新缓存 key:user:userId:cart

        // 将数据放入缓存 key:cartKey hk:skuId hv:cartInfo
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(), cartInfo);

        // 设置过期时间
        setCartKeyExpire(cartKey);
    }

    // 通过用户Id查询购物车列表
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isEmpty(userId)) {
            //未登录，获取未登录购物车数据
            cartInfoList = getCartList(userTempId);
        }
        /*
        合并购物车，判断未登录时购物车中是否有数据
        有：将skuId相同的进行数量相加，不用的直接添加
        没有：直接查询登录状态下的数据
         */
        // 已登录
        if (!StringUtils.isEmpty(userId)) {
            // 获取临时用户购物车数据
            List<CartInfo> cartList = getCartList(userTempId);
            if (!CollectionUtils.isEmpty(cartList)) {
                // 进行合并
                cartInfoList = mergeToCartList(cartList, userId);
                // 删除未登录购物车数据
                deleteCartList(userTempId);
            }
            // 未登录时，购物车是空的，或者根本没有临时用户Id
            if (CollectionUtils.isEmpty(cartList) || StringUtils.isEmpty(userTempId)) {
                // 未登录状态下，购物车中没有数据
                cartInfoList = getCartList(userId);
            }
        }
        return cartInfoList;
    }

    // 更改选中状态
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        // update cate_info set is_checked = ? where user_id = ? and sku_id = ?
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);

        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId).eq("sku_id", skuId);
        cartInfoMapper.update(cartInfo, cartInfoQueryWrapper);

        // 修改缓存
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())) {
            CartInfo cartInfoUpd = (CartInfo) boundHashOperations.get(skuId.toString());
            // 修改选中状态
            cartInfoUpd.setIsChecked(isChecked);
            // 存入缓存
            boundHashOperations.put(skuId.toString(), cartInfoUpd);
            // 设置过期时间
            setCartKeyExpire(cartKey);
        }
    }

    // 删除已登录购物车数据
    @Override
    public void deleteCart(Long skuId, String userId) {
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("sku_id", skuId).eq("user_id", userId);
        cartInfoMapper.delete(cartInfoQueryWrapper);

        // 删除缓存
        String cartKey = getCartKey(userId);
        BoundHashOperations<String, String, CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())) {
            boundHashOperations.delete(skuId.toString());
        }
    }

    // 根据用户id查询购物车列表
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        // 直接查询缓存即可
        List<CartInfo> cartInfoList = new ArrayList<>();

        String cartKey = getCartKey(userId);
        List<CartInfo> cartCachInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartCachInfoList)) {
            for (CartInfo cartInfo : cartCachInfoList) {
                if (cartInfo.getIsChecked() == 1) {
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }

    // 删除未登录购物车数据
    private void deleteCartList(String userTempId) {
        // 删除数据 mysql + redis
        // delete from cart_info where user_id = ?
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userTempId);
        cartInfoMapper.delete(cartInfoQueryWrapper);

        // 删除缓存
        // 获取key
        String cartKey = getCartKey(userTempId);
        // 判断key是否存在
        if (redisTemplate.hasKey(cartKey)) {
            redisTemplate.delete(cartKey);
        }

    }

    // 合并购物车
    private List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId) {
        // 获取到用户登录购物车数据
        List<CartInfo> cartListLogin = getCartList(userId);
        // 将cartListLogin登录的用户数据集合转换成map集合，key：skuId value：cartInfo
        Map<Long, CartInfo> cartInfoMapLogin = cartListLogin.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        // 循环未登录时购物车的数据，根据商品Id进行判断
        for (CartInfo cartInfoNoLogin : cartList) {
            // 获取未登录时购物车中的商品Id
            Long skuId = cartInfoNoLogin.getSkuId();
            // 判断登录后购物车的skuId是否相等
            if (cartInfoMapLogin.containsKey(skuId)) {
                // 有相同商品，进行数量相加
                CartInfo cartInfoLogin = cartInfoMapLogin.get(skuId);
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum() + cartInfoNoLogin.getSkuNum());
                // 合并时需要注意购物车选中状态
                // 如果未登录时购物车中的商品被选中，登录后也要是选中
                if (cartInfoNoLogin.getIsChecked().intValue() == 1) {
                    cartInfoLogin.setIsChecked(1);
                }
                // 更新数据库
                cartInfoMapper.updateById(cartInfoLogin);
            } else {
                // 未登录和登录之后购物车没有相同商品，直接添加
                // 修改临时用户Id为已登录的用户Id
                cartInfoNoLogin.setId(null);
                cartInfoNoLogin.setUserId(userId);
                // 插入数据库
                cartInfoMapper.insert(cartInfoNoLogin);
            }
        }
        // 数据汇总
        List<CartInfo> cartInfoList = loadCartCache(userId);

        return cartInfoList;
    }

    // 根据用户id，获取购物车数据集合
    private List<CartInfo> getCartList(String userId) {
        /*
        先查询缓存，如果缓存中没有数据，再查数据库，并把数据放入缓存
         */
        // 声明一个list集合
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isEmpty(userId)) return cartInfoList;
        // 查询缓存，获取key
        String cartKey = getCartKey(userId);
        // 根据key查询缓存
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            // 缓存中有数据
            // 购物车列表展示的时候，有顺序，按照upda_time来排序
            // 模拟对商品的排序，按照Id排序 | 实际开发中应按照updateTime
            cartInfoList.sort((o1, o2) -> {
                // Long String 比较方法
                return o1.getId().compareTo(o2.getId());
            });
            return cartInfoList;
        } else {
            // 缓存中没有数据，需查询数据库
            cartInfoList = loadCartCache(userId);

            return cartInfoList;
        }
    }

    // 根据用户Id查询数据库中数据，并放入缓存
    @Override
    public List<CartInfo> loadCartCache(String userId) {
        // select * from cart_info where user_id = ?
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(cartInfoQueryWrapper);
        if (CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList;
        }
        // 数据库中有数据，将数据放入缓存
        // 获取key
        String cartKey = getCartKey(userId);
        // 声明一个map集合 key:skuId value:cartInfo
        HashMap<String, CartInfo> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            // 查询数据库，说明有可能缓存过期了，价格可能出现变动
            // 查询最新的价格
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            // 将数据放入map
            map.put(cartInfo.getSkuId().toString(), cartInfo);
        }
        // 存入缓存
        redisTemplate.opsForHash().putAll(cartKey, map);
        // 设置过期时间
        setCartKeyExpire(cartKey);

        return cartInfoList;
    }

    // 设置过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    // 获取购物车的key
    private String getCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
