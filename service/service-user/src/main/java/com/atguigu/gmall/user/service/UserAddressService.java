package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-28 下午 18:12
 */
public interface UserAddressService extends IService<UserAddress> {

    /**
     * 根据用户id查询用户收货地址列表
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(String userId);
}
