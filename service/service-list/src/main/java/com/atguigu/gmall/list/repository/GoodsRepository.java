package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author zr
 * @create 2020-03-23 下午 21:13
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods, Long> {
}
