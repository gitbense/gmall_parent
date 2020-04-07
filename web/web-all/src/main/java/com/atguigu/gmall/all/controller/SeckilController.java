package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author zr
 * 秒杀
 * @create 2020-04-06 下午 18:40
 */
@Controller
public class SeckilController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    /**
     * 秒杀列表
     *
     * @param model
     * @return
     */
    @GetMapping("seckill.html")
    public String index(Model model) {
        Result result = activityFeignClient.findAll();
        model.addAttribute("list", result.getData());

        return "seckill/index";
    }

    /**
     * 秒杀详情
     *
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("seckill/{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model) {
        Result result = activityFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item", result.getData());

        return "seckill/item";
    }

    // 秒杀排队
    // window.location.href = '/seckill/queue.html?skuId='+this.skuId+'&skuIdStr='+skuIdStr
    @GetMapping("seckill/queue.html")
    public String queue(@RequestParam(name = "skuId") Long skuId,
                        @RequestParam(name = "skuIdStr") String skuIdStr,
                        HttpServletRequest request) {
        // 保存商品Id，下单码
        request.setAttribute("skuId", skuId);
        request.setAttribute("skuIdStr", skuIdStr);

        // 返回排队页面
        return "seckill/queue";
    }

    @GetMapping("seckill/trade.html")
    public String trade(Model model) {
        // 获取接口数据
        Result<Map<String, Object>> result = activityFeignClient.trade();
        if (result.isOk()) {
            // 保存数据
            model.addAllAttributes(result.getData());
            return "seckill/trade";
        } else {
            model.addAttribute("message", result.getMessage());
            return "seckill/fail";
        }
    }
}
