package com.team.lms.mapper;

import com.team.lms.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {
    Category selectById(Long id);
    Category selectByCode(String code);
    List<Category> selectAllActive();
    List<Category> selectAll();
    int insert(Category category);
    int update(Category category);
    void softDeleteById(Long id);
}
