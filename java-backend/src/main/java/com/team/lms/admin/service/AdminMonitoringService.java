package com.team.lms.admin.service;

import com.team.lms.admin.dto.AdminBackupRestoreRequest;
import com.team.lms.admin.vo.AdminBackupRecordVo;
import com.team.lms.admin.vo.AdminBusinessReportVo;
import com.team.lms.admin.vo.AdminFineStatisticsVo;
import com.team.lms.admin.vo.AdminMonitoringOverviewVo;
import com.team.lms.admin.vo.AdminOperationLogVo;
import com.team.lms.admin.vo.AdminRestoreResultVo;
import com.team.lms.admin.vo.AdminRuntimeStatusVo;

import java.util.List;

public interface AdminMonitoringService {
    AdminMonitoringOverviewVo getOverview(String authorizationHeader);
    AdminBusinessReportVo getBusinessReport(String authorizationHeader);
    AdminFineStatisticsVo getFineStatistics(String authorizationHeader);
    AdminRuntimeStatusVo getRuntimeStatus(String authorizationHeader);
    List<AdminOperationLogVo> listOperationLogs(String authorizationHeader);
    List<AdminOperationLogVo> listAbnormalBehaviors(String authorizationHeader);
    AdminBackupRecordVo executeBackup(String authorizationHeader);
    List<AdminBackupRecordVo> listBackupRecords(String authorizationHeader);
    AdminRestoreResultVo restoreBackup(String authorizationHeader, AdminBackupRestoreRequest request);
}
