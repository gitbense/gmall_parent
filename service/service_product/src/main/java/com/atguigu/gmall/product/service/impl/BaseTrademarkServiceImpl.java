package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zr
 * @create 2020-03-14 下午 17:12
 */
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper, BaseTrademark> implements BaseTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    //品牌列表分页查询
    @Override
    public IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam) {
        QueryWrapper<BaseTrademark> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("id");
        return baseTrademarkMapper.selectPage(pageParam, wrapper);
    }

    //查询全部品牌
    @Override
    public List<BaseTrademark> getTrademarkList() {
        return baseTrademarkMapper.selectList(null);
    }
}
