package com.team.lms.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminDictionaryCreateRequest {
    @NotBlank(message = "dictType is required")
    private String dictType;

    @NotBlank(message = "dictCode is required")
    private String dictCode;

    @NotBlank(message = "dictName is required")
    private String dictName;

    private String businessValue;
    private Integer sortOrder;
    private Boolean enabled;
    private String description;
}
