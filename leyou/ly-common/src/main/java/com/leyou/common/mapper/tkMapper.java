package com.leyou.common.mapper;

import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.additional.insert.InsertListMapper;//主键任意
import tk.mybatis.mapper.annotation.RegisterMapper;
import tk.mybatis.mapper.common.Mapper;


/**
 * 作为通用mapper的总接口,去继承
 * @param <T>
 */
@RegisterMapper //让通用mapper扫描到
public interface tkMapper<T> extends Mapper<T> , IdListMapper<T,Long>, InsertListMapper<T> {
        }
