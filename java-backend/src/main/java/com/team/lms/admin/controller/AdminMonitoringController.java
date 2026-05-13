package com.team.lms.admin.controller;

import com.team.lms.admin.dto.AdminBackupRestoreRequest;
import com.team.lms.admin.service.AdminMonitoringService;
import com.team.lms.admin.vo.AdminBackupRecordVo;
import com.team.lms.admin.vo.AdminBusinessReportVo;
import com.team.lms.admin.vo.AdminMonitoringOverviewVo;
import com.team.lms.admin.vo.AdminOperationLogVo;
import com.team.lms.admin.vo.AdminRestoreResultVo;
import com.team.lms.admin.vo.AdminRuntimeStatusVo;
import com.team.lms.common.api.ApiResponse;
import com.team.lms.common.api.BaseController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/monitoring")
public class AdminMonitoringController extends BaseController {

    private final AdminMonitoringService adminMonitoringService;

    @GetMapping("/overview")
    public ApiResponse<AdminMonitoringOverviewVo> getOverview(@RequestHeader("Authorization") String authorizationHeader) {
        return success(adminMonitoringService.getOverview(authorizationHeader));
    }

    @GetMapping("/reports")
    public ApiResponse<AdminBusinessReportVo> getBusinessReport(@RequestHeader("Authorization") String authorizationHeader) {
        return success(adminMonitoringService.getBusinessReport(authorizationHeader));
    }

    @GetMapping("/runtime-status")
    public ApiResponse<AdminRuntimeStatusVo> getRuntimeStatus(@RequestHeader("Authorization") String authorizationHeader) {
        return success(adminMonitoringService.getRuntimeStatus(authorizationHeader));
    }

    @GetMapping("/operation-logs")
    public ApiResponse<List<AdminOperationLogVo>> listOperationLogs(@RequestHeader("Authorization") String authorizationHeader) {
        return success(adminMonitoringService.listOperationLogs(authorizationHeader));
    }

    @GetMapping("/abnormal-behaviors")
    public ApiResponse<List<AdminOperationLogVo>> listAbnormalBehaviors(@RequestHeader("Authorization") String authorizationHeader) {
        return success(adminMonitoringService.listAbnormalBehaviors(authorizationHeader));
    }

    @PostMapping("/backups")
    public ApiResponse<AdminBackupRecordVo> executeBackup(@RequestHeader("Authorization") String authorizationHeader) {
        return success(adminMonitoringService.executeBackup(authorizationHeader));
    }

    @GetMapping("/backups")
    public ApiResponse<List<AdminBackupRecordVo>> listBackupRecords(@RequestHeader("Authorization") String authorizationHeader) {
        return success(adminMonitoringService.listBackupRecords(authorizationHeader));
    }

    @PostMapping("/backups/restore")
    public ApiResponse<AdminRestoreResultVo> restoreBackup(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody AdminBackupRestoreRequest request
    ) {
        return success(adminMonitoringService.restoreBackup(authorizationHeader, request));
    }
}
