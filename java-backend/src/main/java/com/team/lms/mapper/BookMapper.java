package com.team.lms.mapper;

import com.team.lms.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookMapper {
    List<Book> selectAll();
    Book selectById(Long id);
    Book selectByIsbn(String isbn);
    void insert(Book book);
    void update(Book book);
    void softDeleteById(Long id);

    long countByCategoryId(@Param("categoryId") Long categoryId);
    void clearCategoryByCategoryId(@Param("categoryId") Long categoryId);
    List<Book> selectAllVisible();
    List<Book> selectVisibleByKeyword(@Param("keyword") String keyword);
}
