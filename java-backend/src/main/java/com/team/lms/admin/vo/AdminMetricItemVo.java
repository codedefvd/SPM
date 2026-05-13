package com.team.lms.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminMetricItemVo {
    private String label;
    private Long value;
}
