package com.team.lms.entity;

import com.team.lms.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataDictionary extends BaseEntity {
    private String dictType;
    private String dictCode;
    private String dictName;
    private String businessValue;
    private Integer sortOrder;
    private Boolean enabled;
    private String description;
}
