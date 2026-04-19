package com.team.lms.admin.service.impl;

import com.team.lms.admin.dto.AdminDictionaryCreateRequest;
import com.team.lms.admin.dto.AdminDictionaryUpdateRequest;
import com.team.lms.admin.service.AdminDictionaryService;
import com.team.lms.admin.vo.AdminDictionaryVo;
import com.team.lms.entity.DataDictionary;
import com.team.lms.exception.BusinessException;
import com.team.lms.mapper.DataDictionaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDictionaryServiceImpl implements AdminDictionaryService {

    private final DataDictionaryMapper dataDictionaryMapper;

    @Override
    public List<AdminDictionaryVo> listDictionaries(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return dataDictionaryMapper.selectAll().stream().map(this::toVo).toList();
        }
        return dataDictionaryMapper.selectByType(normalizeToken(dictType)).stream().map(this::toVo).toList();
    }

    @Override
    @Transactional
    public AdminDictionaryVo createDictionary(AdminDictionaryCreateRequest request) {
        String normalizedType = normalizeToken(request.getDictType());
        String normalizedCode = normalizeToken(request.getDictCode());

        if (dataDictionaryMapper.selectByTypeAndCode(normalizedType, normalizedCode) != null) {
            throw new BusinessException(400, "dictionary code already exists under this type");
        }

        DataDictionary dictionary = new DataDictionary();
        dictionary.setDictType(normalizedType);
        dictionary.setDictCode(normalizedCode);
        dictionary.setDictName(request.getDictName().trim());
        dictionary.setBusinessValue(trimToNull(request.getBusinessValue()));
        dictionary.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        dictionary.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
        dictionary.setDescription(trimToNull(request.getDescription()));

        dataDictionaryMapper.insert(dictionary);
        return toVo(dictionary);
    }

    @Override
    @Transactional
    public AdminDictionaryVo updateDictionary(Long dictionaryId, AdminDictionaryUpdateRequest request) {
        DataDictionary dictionary = dataDictionaryMapper.selectById(dictionaryId);
        if (dictionary == null) {
            throw new BusinessException(404, "dictionary entry not found");
        }

        dictionary.setDictName(request.getDictName().trim());
        dictionary.setBusinessValue(trimToNull(request.getBusinessValue()));
        dictionary.setSortOrder(request.getSortOrder() == null ? dictionary.getSortOrder() : request.getSortOrder());
        dictionary.setEnabled(request.getEnabled() == null ? dictionary.getEnabled() : request.getEnabled());
        dictionary.setDescription(trimToNull(request.getDescription()));

        dataDictionaryMapper.update(dictionary);
        return toVo(dataDictionaryMapper.selectById(dictionaryId));
    }

    @Override
    @Transactional
    public void deleteDictionary(Long dictionaryId) {
        DataDictionary dictionary = dataDictionaryMapper.selectById(dictionaryId);
        if (dictionary == null) {
            throw new BusinessException(404, "dictionary entry not found");
        }
        dataDictionaryMapper.softDeleteById(dictionaryId);
    }

    private String normalizeToken(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AdminDictionaryVo toVo(DataDictionary item) {
        return AdminDictionaryVo.builder()
                .dictionaryId(item.getId())
                .dictType(item.getDictType())
                .dictCode(item.getDictCode())
                .dictName(item.getDictName())
                .businessValue(item.getBusinessValue())
                .sortOrder(item.getSortOrder())
                .enabled(item.getEnabled())
                .description(item.getDescription())
                .build();
    }
}
