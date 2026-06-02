package com.team.lms.mapper;

import com.team.lms.entity.ReviewReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewReplyMapper {
    List<ReviewReply> selectByReviewId(@Param("reviewId") Long reviewId);
    ReviewReply selectById(@Param("id") Long id);
    void insert(ReviewReply reply);
    void update(ReviewReply reply);
    void deleteById(@Param("id") Long id);
}
