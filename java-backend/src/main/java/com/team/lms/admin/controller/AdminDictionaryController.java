package com.team.lms.admin.controller;

import com.team.lms.admin.dto.AdminDictionaryCreateRequest;
import com.team.lms.admin.dto.AdminDictionaryUpdateRequest;
import com.team.lms.admin.service.AdminDictionaryService;
import com.team.lms.admin.vo.AdminDictionaryVo;
import com.team.lms.common.api.ApiResponse;
import com.team.lms.common.api.BaseController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dictionaries")
public class AdminDictionaryController extends BaseController {

    private final AdminDictionaryService adminDictionaryService;

    @GetMapping
    public ApiResponse<List<AdminDictionaryVo>> listDictionaries(
            @RequestParam(value = "dict_type", required = false) String dictType
    ) {
        return success(adminDictionaryService.listDictionaries(dictType));
    }

    @PostMapping
    public ApiResponse<AdminDictionaryVo> createDictionary(@Valid @RequestBody AdminDictionaryCreateRequest request) {
        return success(adminDictionaryService.createDictionary(request));
    }

    @PutMapping("/{dictionaryId}")
    public ApiResponse<AdminDictionaryVo> updateDictionary(
            @PathVariable Long dictionaryId,
            @Valid @RequestBody AdminDictionaryUpdateRequest request
    ) {
        return success(adminDictionaryService.updateDictionary(dictionaryId, request));
    }

    @DeleteMapping("/{dictionaryId}")
    public ApiResponse<Void> deleteDictionary(@PathVariable Long dictionaryId) {
        adminDictionaryService.deleteDictionary(dictionaryId);
        return success(null);
    }
}
