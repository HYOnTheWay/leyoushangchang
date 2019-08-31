package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 分类管理的商品
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;


    public List<Category> queryCategoryListByPid(Long pid) {
        Category category = new Category();
        //mapper会把对象中的非空属性作为查询条件
        category.setParentId(pid);
        List<Category> list = categoryMapper.select(category);
        //用工具类判断是否为空
        if (CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return list;
    }

    /**
     * 根据id集合查询品牌集合
     * @param ids
     * @return
     */
    public List<Category> queryByIds(List<Long> ids){
        List<Category> categoriesList = categoryMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(categoriesList)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return categoriesList;
    }
}
