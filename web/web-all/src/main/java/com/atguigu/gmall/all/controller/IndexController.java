package com.atguigu.gmall.all.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author zr
 * @create 2020-03-23 下午 19:24
 */
@Api(tags = "gmall首页")
@Controller
@RequestMapping
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private SpringTemplateEngine templateEngine;

    /**
     * 利用模板引擎自动生成一个静态页面，可以直接将页面放入nginx中
     * @return
     * @throws IOException
     */
    @GetMapping("createHtml")
    @ResponseBody
    public Result createHtml() throws IOException {
        // 自动渲染数据
        Result<List<JSONObject>> result = productFeignClient.getBaseCategoryList();
        // 声明一个context对象
        Context context = new Context();
        context.setVariable("list",result.getData());
        // 输入页面
        FileWriter fileWriter = new FileWriter("E:\\index.html");
        // 利用模板引擎创建
        templateEngine.process("index/index.html",context,fileWriter);
        return Result.ok();
    }

    @GetMapping({"/","index.html"})
    public String index(HttpServletRequest request) {
        Result<List<JSONObject>> result = productFeignClient.getBaseCategoryList();
        request.setAttribute("list",result.getData());
        // 返回的视图名称
        return "index/index";
    }

}
