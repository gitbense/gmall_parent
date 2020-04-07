package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author zr
 * @create 2020-03-27 下午 16:00
 */
@Api(tags = "购物车")
@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    @ApiOperation("添加购物车")
    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request) {
        // 获取userId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            // 用户未登录，获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addToCart(skuId, userId, skuNum);
        return Result.ok();
    }

    @ApiOperation("查询购物车")
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);

        List<CartInfo> cartList = cartService.getCartList(userId, userTempId);
        return Result.ok(cartList);
    }

    @ApiOperation("更新选中状态")
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(userId, isChecked, skuId);

        return Result.ok();
    }

    @ApiOperation("删除购物车一条数据")
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(skuId, userId);

        return Result.ok();
    }

    /**
     * 根据用户id查询购物车列表
     *
     * @param userId
     * @return
     */
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId) {
        return cartService.getCartCheckedList(userId);
    }

    @GetMapping("loadCartCache/{userId}")
    public Result loadCartCache(@PathVariable String userId) {
        cartService.loadCartCache(userId);
        return Result.ok();
    }
}
