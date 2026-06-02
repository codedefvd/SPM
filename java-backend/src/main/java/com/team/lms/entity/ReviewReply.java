package com.team.lms.entity;

import com.team.lms.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewReply extends BaseEntity {
    private BookReview review;
    private User reader;
    private String replyContent;
}
