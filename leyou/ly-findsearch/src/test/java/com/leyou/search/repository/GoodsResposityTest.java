package com.leyou.search.repository;

import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Spu;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GoodsResposityTest {

    @Autowired
    private GoodsResposity goodsResposity;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SearchService searchService;

    /**
     * 用来做映射
     */
    @Test
    public void testCreateIndex(){
        template.createIndex(Goods.class);
        template.putMapping(Goods.class);
    }

    /**
     * 用来将信息添加进索引
     */
    @Test
    public void loadData(){
        int page = 1;
        int rows = 100;
        int size = 0;
        do {
            //查询spu
            PageResult<Spu> spuPageResult = goodsClient.querySpuByPage(page, rows, true, null);
            List<Spu> spuList = spuPageResult.getItems();
            if (CollectionUtils.isEmpty(spuList)){
                break;
            }
            //构建成goods
            List<Goods> goodList = spuList.stream().map(searchService::buildGoods).collect(Collectors.toList());
            //存入索引库
            goodsResposity.saveAll(goodList);
            //翻页
            page++;
            size = spuList.size();
        }while (size == 100);
    }
}