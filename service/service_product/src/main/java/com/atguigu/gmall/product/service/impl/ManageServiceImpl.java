package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zr
 * @create 2020-03-13 下午 15:04
 */
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;

    //获取一级分类
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    //获取二级分类
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        wrapper.eq("category1_id", category1Id);
        List<BaseCategory2> category2List = baseCategory2Mapper.selectList(wrapper);
        return category2List;
    }

    //获取三级分类
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();
        wrapper.eq("category2_id", category2Id);
        List<BaseCategory3> category3List = baseCategory3Mapper.selectList(wrapper);
        return category3List;
    }

    //根据分类id获取平台属性
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    //添加or修改平台属性
    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //判断
        if (baseAttrInfo.getId() != null) {
            //修改属性
            baseAttrInfoMapper.updateById(baseAttrInfo);
        } else {
            //添加属性
            baseAttrInfoMapper.insert(baseAttrInfo);
        }

        //修改属性值，先删除，再添加
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id", baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueQueryWrapper);

        //添加属性值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList != null && attrValueList.size() > 0) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    //根据平台属性ID获取平台属性
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {

        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //查询最新的平台属性集合值放入到集合中
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    //spu分页查询
    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        spuInfoQueryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(pageParam, spuInfoQueryWrapper);
    }

    //查询所有销售属性数据
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    //保存商品数据
    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //spuInfo商品表
        spuInfoMapper.insert(spuInfo);
        //spuImage商品图片表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
        //spuSaleAttr 销售属性表
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //SpuSaleAttrValue销售属性值表
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        //处理销售属性值中的name属性
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    //根据spuId查询商品图片列表
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> spuImageQueryWrapper = new QueryWrapper<>();
        spuImageQueryWrapper.eq("spu_id", spuId);
        return spuImageMapper.selectList(spuImageQueryWrapper);
    }

    //根据spuId查询销售属性集合
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    //保存sku
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        //sku_info 库存单元表
        skuInfoMapper.insert(skuInfo);
        //sku_image 库存单元图片表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //sku_attr_value sku平台属性值
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }
        //sku_sale_attr_value sku销售属性值
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                //spuId页面提交给skuInfo了，可以直接从skuInfo中获取
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
        // 商品上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuInfo.getId());
    }

    //SKU分页列表
    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam) {
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(pageParam, skuInfoQueryWrapper);
    }

    //商品上架
    @Override
    @Transactional
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);

        // 商品上架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);
    }

    //商品下架
    @Override
    @Transactional
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);

        // 商品下架
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);
    }

    //根据skuId获取skuInfo
    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX) //sku:
    public SkuInfo getSkuInfo(Long skuId) {
        //return getSkuInfoRedisson(skuId);
        //return getSkuInfoRedis(skuId);
        return getSkuInfoDB(skuId);
    }

    //使用Redisson解决分布式锁
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //定义key sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //查询缓存中的数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //判断
            if (skuInfo == null) {
                //缓存中无此数据，需查数据库，加锁，防止缓存击穿
                //lockKey sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (res) {
                    try {
                        System.out.println("获取到分布式锁");
                        //从数据库查询数据，放入缓存
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo == null) {
                            //数据库中无此数据，new一个空对象放入缓存，防止缓存穿透
                            SkuInfo skuInfo1 = new SkuInfo();
                            redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        //缓存中有数据，直接放入缓存
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        //解锁
                        lock.unlock();
                    }
                } else {
                    //没有拿到锁，等待一会儿，再尝试
                    Thread.sleep(1000);
                    getSkuInfoRedisson(skuId);
                }
            } else {
                //如果用户在第一次查询时，缓存和数据库都没有数据，会直接new一个空对象放入缓存
                //此时，在第二次查询时会走redis，查询到空对象，直接返回null，防止缓存穿透
                if (null == skuInfo.getId()) {
                    return null;
                }
                //缓存中有数据，直接返回
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //如果缓存宕机，直接从数据库中查询
        return getSkuInfoDB(skuId);
    }

    //使用redis，解决分布式锁
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //定义key sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //查询缓存中的数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if (skuInfo == null) {
                //缓存没有数据，查询数据库，加锁，防止缓存击穿
                //set k1 v1 px 10000 nx
                //sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                String uuid = UUID.randomUUID().toString().replace("-", "");
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (isExist) {
                    System.out.println("获取到了分布式锁");
                    //从数据库查询数据，并放入缓存
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo == null) {
                        //数据库无此数据，new一个空对象放入缓存，防止缓存穿透
                        SkuInfo skuInfo1 = new SkuInfo();
                        //放入缓存
                        redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    //查到了数据，放入缓存
                    redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    //删除锁
                    //                if (uuid.equals(redisTemplate.opsForValue().get(lockKey))) {
                    //                    redisTemplate.delete(lockKey);
                    //                }
                    //使用lua脚本来防止误删key
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    //设置lua脚本返回的数据类型
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);
                    return skuInfo;
                } else {
                    //没有拿到锁，等待一会儿，再调用方法
                    Thread.sleep(500);
                    return getSkuInfoRedis(skuId);
                }

            } else {
                //如果用户在第一次查询时，缓存和数据库都没有数据，会直接new一个空对象放入缓存
                //此时，在第二次查询时会走redis，查询到空对象，直接返回null，防止缓存穿透
                if (null == skuInfo.getId()) {
                    return null;
                }
                //缓存中有数据，直接返回
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //如果缓存宕机，直接从数据库中查询
        return getSkuInfoDB(skuId);
    }

    //根据skuId获取skuInfo（数据库）
    private SkuInfo getSkuInfoDB(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
            skuImageQueryWrapper.eq("sku_id", skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageQueryWrapper);
            skuInfo.setSkuImageList(skuImageList);
        }

        return skuInfo;
    }

    //通过三级分类id查询分类信息
    @Override
    @GmallCache(prefix = "categoryView")
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        //select * from base_category_view where id = category3Id
        return baseCategoryViewMapper.selectById(category3Id);
    }

    //获取sku价格
    @Override
    @GmallCache(prefix = "skuPrice")
    public BigDecimal getSkuPrice(Long skuId) {
        //select price from sku_info where sku_id = skuId
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            return skuInfo.getPrice();
        }
        return new BigDecimal(0);
    }

    //根据spuId，skuId 查询销售属性集合
    @Override
    @GmallCache(prefix = "spuSaleAttr")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    //根据spuId 查询map 集合属性
    @Override
    @GmallCache(prefix = "skuValue")
    public Map getSkuValueIdsMap(Long spuId) {
        Map<Object, Object> map = new HashMap<>();
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        if (mapList != null && mapList.size() > 0) {
            //循环遍历
            for (Map skuMap : mapList) {
                map.put(skuMap.get("value_ids"), skuMap.get("sku_id"));
            }
        }
        return map;
    }

    // 获取全部分类信息
    @Override
    @GmallCache(prefix = "category")
    public List<JSONObject> getBaseCategoryList() {
        // 声明json集合对象
        List<JSONObject> list = new ArrayList<>();
        // 查询所有分类数据
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        // 按照一级分类id进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        // 开始准备构建
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            // 初始化一级分类对象 categoryId categoryName 等数据进行初始化
            // 获取一级分类id
            Long category1Id = entry1.getKey();
            // 获取一级分类下的所有集合数据
            List<BaseCategoryView> category2List = entry1.getValue();
            // System.out.println("----------------" + category2List);
            // [BaseCategoryView(category1Id=1, category1Name=图书、音像、电子书刊,
            //category2Id=1, category2Name=电子书刊,
            //category3Id=1, category3Name=电子书),

            JSONObject category1 = new JSONObject();
            // 赋值
            category1.put("index", index);
            category1.put("categoryId", category1Id);
            category1.put("categoryName", category2List.get(0).getCategory1Name());
            // TODO categoryChild
            //category1.put("categoryChild",{});

            // 迭代index
            index++;
            // 获取二级分类下数据集合
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            // 声明一个二级分类的集合对象
            List<JSONObject> category2Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                // 获取二级分类对象，进行赋值
                JSONObject category2 = new JSONObject();
                // 获取二级分类id
                Long category2Id = entry2.getKey();
                // 获取二级分类下的所有数据
                List<BaseCategoryView> category3List = entry2.getValue();
                // 赋值
                category2.put("categoryId", category2Id);
                category2.put("categoryName", category3List.get(0).getCategory2Name());
                //category2.put("categoryChild",{});
                category2Child.add(category2);

                // 处理三级分类数据
                // 声明一个三级分类数据集合
                List<JSONObject> category3Child = new ArrayList<>();
                // 循环获取三级分类数据
                category3List.stream().forEach(category3View -> {
                    // 声明一个三级分类对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", category3View.getCategory3Id());
                    category3.put("categoryName", category3View.getCategory3Name());
                    // 将每个三级分类对象添加到集合
                    category3Child.add(category3);
                });

                category2.put("categoryChild", category3Child);
            }
            category1.put("categoryChild", category2Child);
            list.add(category1);
        }
        return list;
    }

    // 通过品牌Id 来查询数据
    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        // select * from base_tradmake where id = tmId
        return baseTrademarkMapper.selectById(tmId);
    }

    // 通过skuId 查询对应的平台属性和属性值
    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }

    //根据平台属性ID获取平台属性值
    private List<BaseAttrValue> getAttrValueList(Long attrId) {
        QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_id", attrId);
        return baseAttrValueMapper.selectList(wrapper);
    }
}
