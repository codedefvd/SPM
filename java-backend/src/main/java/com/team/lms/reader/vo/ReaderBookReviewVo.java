package com.team.lms.reader.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReaderBookReviewVo {
    private Long reviewId;
    private String readerUsername;
    private Integer ratingScore;
    private String reviewContent;
    private String createdAt;
    private Boolean mine;
    private Integer likeCount;
    private Boolean likedByMe;
    private List<ReaderReviewReplyVo> replies;
}
