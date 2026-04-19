package com.team.lms.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogQueryDTO {
    private String operatorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String actionName;
    private Integer page;
    private Integer size;
}