package com.team.lms.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminRuntimeStatusVo {
    private String systemStatus;
    private String databaseStatus;
    private String backupStatus;
    private Integer recentFailureLogs;
    private Integer disabledUsers;
    private Integer booksWithoutAvailableCopies;
    private Integer activeUsersInLast24Hours;
    private Integer latestOperationLogId;
    private List<AdminMetricItemVo> alerts;
}
