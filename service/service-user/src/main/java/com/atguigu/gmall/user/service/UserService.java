package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author zr
 * @create 2020-03-25 下午 12:26
 */
public interface UserService {

    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);
}
