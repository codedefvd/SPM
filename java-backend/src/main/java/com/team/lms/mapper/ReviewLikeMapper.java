package com.team.lms.mapper;

import com.team.lms.entity.ReviewLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewLikeMapper {
    ReviewLike selectByReviewAndReader(@Param("reviewId") Long reviewId, @Param("readerId") Long readerId);
    int countByReviewId(@Param("reviewId") Long reviewId);
    void insert(ReviewLike reviewLike);
    void deleteById(@Param("id") Long id);
}
