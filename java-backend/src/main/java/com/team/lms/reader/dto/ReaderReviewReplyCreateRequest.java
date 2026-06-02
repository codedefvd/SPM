package com.team.lms.reader.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReaderReviewReplyCreateRequest {
    @NotBlank(message = "reply content is required")
    private String replyContent;
}
