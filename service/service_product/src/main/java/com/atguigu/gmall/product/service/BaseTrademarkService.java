package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-14 下午 17:05
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {

    /**
     * 品牌列表分页查询
     * @param pageParam
     * @return
     */
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam);

    /**
     * 查询全部品牌
     * @return
     */
    List<BaseTrademark> getTrademarkList();
}
