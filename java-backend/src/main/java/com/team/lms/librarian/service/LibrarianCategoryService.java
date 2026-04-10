package com.team.lms.librarian.service;

import com.team.lms.librarian.dto.CategoryCreateRequest;
import com.team.lms.librarian.dto.CategoryUpdateRequest;
import com.team.lms.librarian.vo.CategoryManageVo;

import java.util.List;

public interface LibrarianCategoryService {
    List<CategoryManageVo> listCategories();
    CategoryManageVo createCategory(CategoryCreateRequest request);
    CategoryManageVo updateCategory(Long categoryId, CategoryUpdateRequest request);
    void deleteCategory(Long categoryId, boolean force);
}
