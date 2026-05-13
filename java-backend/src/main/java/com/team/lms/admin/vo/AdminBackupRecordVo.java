package com.team.lms.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminBackupRecordVo {
    private Long backupId;
    private String backupName;
    private String filePath;
    private String status;
    private String summary;
    private String createdAt;
    private Boolean restorable;
}
