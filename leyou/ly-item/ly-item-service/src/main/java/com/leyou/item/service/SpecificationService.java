package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数和规格分类
 */
@Slf4j
@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    public List<SpecGroup> queryGroupByCid(Long cid) {
        //条件
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        //查询
        List<SpecGroup> list = specGroupMapper.select(specGroup);
        if (CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return list;
    }

    public List<SpecParam> queryParamList(Long gid,Long cid,Boolean searching) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setSearching(searching);

        List<SpecParam> list = specParamMapper.select(specParam);
        if (CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.PARAM_GROUP_NOT_FOUND);
        }
        return list;
    }

    public List<SpecGroup> queryListByCid(Long cid) {
        //查询规格组
        List<SpecGroup> specGroups = queryGroupByCid(cid);
        //查询当前分类下的参数
        List<SpecParam> specParams = queryParamList(null, cid, null);

        Map<Long,List<SpecParam>> map = new HashMap<>();
        for (SpecParam specParam : specParams){
            if (!map.containsKey(specParam.getGroupId())){
                //这个组id在map中不存在,新增一个list
                map.put(specParam.getGroupId(),new ArrayList<>());
            }
            map.get(specParam.getGroupId()).add(specParam);
        }
        //填充param到group中
        for (SpecGroup specGroup : specGroups){
            specGroup.setParams(map.get(specGroup.getId()));
        }
        return specGroups;
    }
}
