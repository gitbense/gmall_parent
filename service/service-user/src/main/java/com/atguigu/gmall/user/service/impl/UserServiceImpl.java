package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author zr
 * @create 2020-03-25 下午 12:27
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    // 登录方法
    @Override
    public UserInfo login(UserInfo userInfo) {
        // select * from user_info where login_name = userInfo.getloginName() and passwd = userInfo.getpassword
        // 密码是加密
        String passwd = userInfo.getPasswd();
        String newPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());

        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name", userInfo.getLoginName());
        userInfoQueryWrapper.eq("passwd", newPasswd);

        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        if (info != null) {
            // 直接返回用户对象
            return info;
        }
        return null;
    }
}
