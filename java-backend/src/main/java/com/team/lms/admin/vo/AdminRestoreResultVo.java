package com.team.lms.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminRestoreResultVo {
    private Long backupId;
    private String backupName;
    private String restoredAt;
    private String restoredBy;
    private List<AdminMetricItemVo> restoredTables;
}
