package com.team.lms.mapper;

import com.team.lms.entity.BackupRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BackupRecordMapper {
    List<BackupRecord> selectAll();
    BackupRecord selectById(Long id);
    BackupRecord selectLatest();
    int insert(BackupRecord backupRecord);
}
