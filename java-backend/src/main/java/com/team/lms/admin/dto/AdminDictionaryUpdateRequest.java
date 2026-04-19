package com.team.lms.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminDictionaryUpdateRequest {
    @NotBlank(message = "dictName is required")
    private String dictName;

    private String businessValue;
    private Integer sortOrder;
    private Boolean enabled;
    private String description;
}
