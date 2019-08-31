package com.leyou.item.api;

import com.leyou.item.pojo.Brand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface BrandApi {
    /**
     * 根据id查询品牌
     * @param id
     * @return
     */
    @GetMapping("/brand/{id}")
    Brand queryBrandById(@PathVariable("id")Long id);

    /**
     * 通过ids查询品牌
     * @param ids
     * @return
     */
    @GetMapping("/brand/brands")
    List<Brand> queryBrandsByIds(@RequestParam("ids")List<Long> ids);
}
