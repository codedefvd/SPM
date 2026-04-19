package com.team.lms.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDictionaryVo {
    private Long dictionaryId;
    private String dictType;
    private String dictCode;
    private String dictName;
    private String businessValue;
    private Integer sortOrder;
    private Boolean enabled;
    private String description;
}
