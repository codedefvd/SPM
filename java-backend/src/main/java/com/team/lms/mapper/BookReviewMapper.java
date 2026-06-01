package com.team.lms.mapper;

import com.team.lms.entity.BookReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookReviewMapper {
    List<BookReview> selectAll();

    List<BookReview> selectByBookId(Long bookId);

    BookReview selectByReaderAndBookId(@Param("readerId") Long readerId, @Param("bookId") Long bookId);

    BookReview selectById(Long id);

    void insert(BookReview review);

    void update(BookReview review);
}
