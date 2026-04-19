package com.team.lms.mapper;

import com.team.lms.entity.DataDictionary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataDictionaryMapper {
    DataDictionary selectById(Long id);
    DataDictionary selectByTypeAndCode(@Param("dictType") String dictType, @Param("dictCode") String dictCode);
    List<DataDictionary> selectAll();
    List<DataDictionary> selectByType(String dictType);
    int insert(DataDictionary dictionary);
    int update(DataDictionary dictionary);
    int softDeleteById(Long id);
}
