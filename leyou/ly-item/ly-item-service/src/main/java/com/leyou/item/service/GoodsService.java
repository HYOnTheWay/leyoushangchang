package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品列表分类
 */
@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;
    
    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        //分页
        PageHelper.startPage(page,rows);
        //过滤
        Example example = new Example(Spu.class);
        //搜索条件过滤
        Example.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title","%"+key+"%");
        }
        //上下架过滤
        if (saleable != null){
            criteria.andEqualTo("saleable",saleable);
        }
        //默认排序
        example.setOrderByClause("last_update_time DESC");

        //查询
        List<Spu> spus = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(spus)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //解析分类和品牌的名称
        loadCategoryAndBrandName(spus);
        //解析分页结果
        PageInfo<Spu> spuPageInfo = new PageInfo<>(spus);
        return new PageResult<>(spuPageInfo.getTotal(),spus);
    }

    /**
     * 拿到分类名称和品牌名称展示
     * @param spus
     */
    private void loadCategoryAndBrandName(List<Spu> spus) {
        for (Spu spu : spus){
            //处理分类名称
            List<String> nameList = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(nameList,"/"));
            //处理品牌名称
            spu.setBname(brandService.queryById(spu.getBrandId()).getName());
        }
    }

    @Transactional
    public void addGoods(Spu spu) {
        //新增spu
        spu.setId(null);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spu.setSaleable(true);
        spu.setValid(false);
        int count = spuMapper.insert(spu);
        if (count != 1){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }

        //新增spuDetail
        SpuDetail detail = spu.getSpuDetail();
        detail.setSpuId(spu.getId());
        spuDetailMapper.insert(detail);
        //新增sku
        saveSkuAndStock(spu);

        //发送mq消息
        amqpTemplate.convertAndSend("item.insert",spu.getId());
    }

    public SpuDetail queryDetailById(Long spuId) {
        SpuDetail detail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (detail == null){
            throw new LyException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        return detail;
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        //查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }

//        //查询库存
//        for (Sku sku1 : skuList){
//            Stock stock = stockMapper.selectByPrimaryKey(sku1.getId());
//            if (stock == null){
//                throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
//            }
//            sku1.setStock(stock.getStock());
//        }

        //方法二:查询库存
        List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        loadStockInSku(ids, skuList);
        return skuList;
    }

    @Transactional
    public void updateGoods(Spu spu) {
        if (spu.getId() == null){
            throw new LyException(ExceptionEnum.GOODS_ID_CANNOT_BE_NULL);
        }
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        //查询sku
        List<Sku> skuList = skuMapper.select(sku);
        if (!CollectionUtils.isEmpty(skuList)){
            //删除sku
            skuMapper.delete(sku);
            //删除stock
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.selectByIdList(ids);
        }
        //修改spu
        spu.setValid(null);
        spu.setSaleable(null);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(null);

        int i = spuMapper.updateByPrimaryKeySelective(spu);
        if (i != 1){
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        //修改detail
        i = spuDetailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if (i != 1){
            throw new LyException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        //新增sku和stock
        saveSkuAndStock(spu);

        //发送mq消息
        amqpTemplate.convertAndSend("item.update",spu.getId());
    }

    /**
     * 用于新增sku和stock
     * @param spu
     */
    private void saveSkuAndStock(Spu spu){
        //定义库存集合
        List<Stock> stockList = new ArrayList<>();
        //新增sku
        List<Sku> skus = spu.getSkus();
        for (Sku sku1 : skus){
            sku1.setCreateTime(new Date());
            sku1.setLastUpdateTime(sku1.getCreateTime());
            sku1.setSpuId(spu.getId());
            int count = skuMapper.insert(sku1);
            if (count != 1){
                throw new LyException(ExceptionEnum.GOODS_SKU_SAVE_ERROR);
            }
            //新增库存
            Stock stock = new Stock();
            stock.setSkuId(sku1.getId());
            stock.setStock(sku1.getStock());
            stockList.add(stock);
        }
        //批量新增库存
        int i = stockMapper.insertList(stockList);

        if (i != stockList.size()){
            throw new LyException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    public Spu querySpuById(Long id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //查询sku
        spu.setSkus(querySkuBySpuId(id));
        //查询detail
        spu.setSpuDetail(queryDetailById(id));
        return spu;
    }

    public List<Sku> querySkuByIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skus)){
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        //方法二:查询库存
        loadStockInSku(ids, skus);
        return skus;
    }

    /**
     * 根据ids查询库存
     * @param ids
     * @param skus
     */
    private void loadStockInSku(List<Long> ids, List<Sku> skus) {
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stockList)) {
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }
        //我们把stock变成一个map,其key是:sku的id,值是:库存值
        Map<Long, Integer> stockMap = stockList.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skus.forEach(sku1 -> sku1.setStock(stockMap.get(sku1.getId())));
    }

    @Transactional
    public void decreaseStock(List<CartDTO> carts) {
        for (CartDTO cart : carts) {
            //减库存
            int count = stockMapper.decreaseStock(cart.getSkuId(), cart.getNum());
            if (count != 1 ){
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
