package com.team.lms.admin.service;

import com.team.lms.admin.dto.AdminDictionaryCreateRequest;
import com.team.lms.admin.dto.AdminDictionaryUpdateRequest;
import com.team.lms.admin.vo.AdminDictionaryVo;

import java.util.List;

public interface AdminDictionaryService {
    List<AdminDictionaryVo> listDictionaries(String dictType);
    AdminDictionaryVo createDictionary(AdminDictionaryCreateRequest request);
    AdminDictionaryVo updateDictionary(Long dictionaryId, AdminDictionaryUpdateRequest request);
    void deleteDictionary(Long dictionaryId);
}
