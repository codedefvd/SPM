package com.team.lms.reader.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReaderReviewReplyVo {
    private Long replyId;
    private String readerUsername;
    private String replyContent;
    private String createdAt;
    private boolean mine;
}
