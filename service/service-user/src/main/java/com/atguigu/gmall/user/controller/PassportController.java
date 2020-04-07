package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户认证接口
 *
 * @author zr
 * @create 2020-03-25 下午 17:27
 */
@RestController
@RequestMapping("api/user/passport")
public class PassportController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 登录
     *
     * @param userInfo
     * @return
     */
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo) {
        UserInfo info = userService.login(userInfo);
        if (info != null) {
            // 登录成功，将用户信息存储在缓存
            String token = UUID.randomUUID().toString().replace("-", "");
            // 声明一个map集合
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", info.getName()); // 用户姓名
            map.put("nickName", info.getNickName()); // 用户昵称
            // 访问其他业务模块时，需要判断用户是否登录。必须从缓存中获取userKey然后获取userId
            map.put("token", token);

            // 缓存中只存userId 数据类型String
            // userKey = user:login:token value = userId
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(userKey, info.getId().toString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            return Result.ok(map);
        } else {
            return Result.fail().message("用户名或密码错误！");
        }
    }

    @GetMapping("logout")
    public Result logout(HttpServletRequest request) {
        // 退出登录。 本质：将缓存中数据删除
        // 登录成功时，将token放入header和cookie
        // 远程调用时cookie无法携带数据，用header存储并获取
        String token = request.getHeader("token");
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        redisTemplate.delete(userKey);

        return Result.ok();
    }
}
