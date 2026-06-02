package com.team.lms.reader.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReaderReviewLikeVo {
    private Long reviewId;
    private int likeCount;
    private boolean likedByMe;
}
