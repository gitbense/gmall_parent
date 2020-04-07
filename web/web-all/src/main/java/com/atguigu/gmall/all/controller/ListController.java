package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zr
 * @create 2020-03-24 下午 21:36
 */
@Controller
@RequestMapping
public class ListController {

    @Autowired
    ListFeignClient listFeignClient;

    @ApiOperation("搜索商品")
    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model) {
        Result<Map> result = listFeignClient.list(searchParam);
        // 后台只存储了检索之后的数据
        // 制作一个Url参数列表
        String urlParam = makeUrlParam(searchParam);

        // 处理品牌条件回显
        String trademarkParam = makeTrademark(searchParam.getTrademark());
        // 处理平台属性条件回显
        List<Map<String, String>> propsParamList = makeProps(searchParam.getProps());
        // 处理排序
        Map<String, Object> orderMap = dealOrder(searchParam.getOrder());

        // 保存数据给页面使用
        model.addAllAttributes(result.getData());
        model.addAttribute("urlParam", urlParam);
        model.addAttribute("searchParam", searchParam);
        model.addAttribute("trademarkParam", trademarkParam);
        model.addAttribute("propsParamList", propsParamList);
        model.addAttribute("orderMap", orderMap);

        return "list/index";
    }

    /**
     * @param searchParam 只要用户发起任何的查询请求，参数都会被封装到当前类中
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        // 做一个拼接字符串
        StringBuilder urlParam = new StringBuilder();
        // http://list.gmall.com/list.html?keyword=小米手机
        if (searchParam.getKeyword() != null) {
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        // 通过分类id{1,2,3}查询
        // http://list.gmall.com/list.html?category1Id=2
        if (searchParam.getCategory1Id() != null) {
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        // http://list.gmall.com/list.html?category2Id=13
        if (searchParam.getCategory2Id() != null) {
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        // http://list.gmall.com/list.html?category3Id=61
        if (searchParam.getCategory3Id() != null) {
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        // 判断品牌 http://list.gmall.com/list.html?category3Id=61&trademark=2:华为
        if (searchParam.getTrademark() != null) {
            if (urlParam.length() > 0) {
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        // 判断平台属性值 http://list.gmall.com/list.html?category3Id=61&trademark=2:华为&props=23:4G:运行内存
        if (searchParam.getProps() != null) {
            for (String prop : searchParam.getProps()) {
                if (urlParam.length() > 0) {
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "list.html?" + urlParam.toString();
    }

    /**
     * 处理品牌条件回显
     *
     * @param trademark
     * @return
     */
    private String makeTrademark(String trademark) {
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = StringUtils.split(trademark, ":");
            if (split == null || split.length == 2) {
                return "品牌：" + split[1];
            }
        }
        return "";
    }

    /**
     * 处理平台属性条件回显
     *
     * @param props
     * @return
     */
    private List<Map<String, String>> makeProps(String[] props) {
        List<Map<String, String>> list = new ArrayList<>();
        //2:v:n
        if (props != null && props.length != 0) {
            for (String prop : props) {
                String[] split = StringUtils.split(prop, ":");
                if (split != null && split.length == 3) {
                    // 声明一个map
                    Map<String, String> map = new HashMap<>();
                    map.put("attrId", split[0]);
                    map.put("attrValue", split[1]);
                    map.put("attrName", split[2]);
                    list.add(map);
                }
            }
        }
        return list;
    }

    private Map<String, Object> dealOrder(String order) {
        Map<String, Object> orderMap = new HashMap<>();
        if (!StringUtils.isEmpty(order)) {
            String[] split = StringUtils.split(order, ":");
            if (split != null && split.length == 2) {
                // 传递的哪个字段
                orderMap.put("type", split[0]);
                // 升序降序
                orderMap.put("sort", split[1]);
            }
        } else {
            orderMap.put("type", "1");
            orderMap.put("sort", "asc");
        }
        return orderMap;
    }
}
