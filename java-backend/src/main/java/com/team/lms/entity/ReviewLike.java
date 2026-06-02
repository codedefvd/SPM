package com.team.lms.entity;

import com.team.lms.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewLike extends BaseEntity {
    private BookReview review;
    private User reader;
}
