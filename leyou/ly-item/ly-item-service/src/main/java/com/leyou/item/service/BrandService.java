package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * 品牌分类
 */
@Service
public class BrandService  {

    @Autowired
    private BrandMapper brandMapper;


    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, String sortBy, Boolean desc, String key) {
        //分页
        PageHelper.startPage(page,rows);
        /*
         * WHERE name LIKE "%X%" OR letter == X
         * ORDER BY id DESC
         */
        //过滤
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)){
            //过滤条件
            example.createCriteria().orLike("name","%"+key+"%")
                    .orEqualTo("letter",key.toUpperCase());
        }
        //排序
            if (StringUtils.isNotBlank(sortBy)){
                String orderByClause = sortBy + (desc? " DESC":" ASC");
                example.setOrderByClause(orderByClause);
            }
        //查询
        List<Brand> list = brandMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        //解析分页结果
        PageInfo<Brand> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(),list);
    }

    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        //新增品牌
        brand.setId(null);
        int count = brandMapper.insert(brand);
        if (count != 1){
            throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
        }
        //新增中间表
        for (Long cid : cids){
            int i = brandMapper.insertCategoryBrand(cid, brand.getId());
            if (i != 1){
                throw new LyException(ExceptionEnum.BRAND_SAVE_ERROR);
            }
        }
    }

    /**
     * 通过id查询所有品牌
     * @param id
     * @return
     */
    public Brand queryById(Long id){
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if (brand == null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand;
    }

    /**
     * 通过cid查询所有品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> list = brandMapper.queryBrandByCid(cid);
        if (CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return list;
    }

    public List<Brand> queryBrandsByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
