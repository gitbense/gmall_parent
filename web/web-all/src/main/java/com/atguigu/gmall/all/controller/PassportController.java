package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户认证接口
 * @author zr
 * @create 2020-03-25 下午 18:54
 */
@Controller
public class PassportController {

    // http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
    @GetMapping("login.html")
    public String login(HttpServletRequest request) {
        // originUrl=http://www.gmall.com/
        // 如果登录成功，跳转http://www.gmall.com/
        String originUrl = request.getParameter("originUrl");
        // 保存http://www.gmall.com/
        request.setAttribute("originUrl",originUrl);

        //返回登录页面
        return "login";
    }
}
