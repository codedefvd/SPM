package com.team.lms.admin.service;

import com.team.lms.entity.OperationLog;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogService {
    void logOperation(String moduleName, String actionName, String operatorName, String resultMessage);
    List<OperationLog> getLogList(String operatorName, LocalDateTime startTime, LocalDateTime endTime, String actionName, Integer page, Integer size);
    Integer getLogCount(String operatorName, LocalDateTime startTime, LocalDateTime endTime, String actionName);
    List<OperationLog> getAllLogs();
}