package com.team.lms.librarian.dto;

import lombok.Data;

@Data
public class CategoryDeleteRequest {
    private Boolean force; // 是否强制删除（如果有图书关联）
}