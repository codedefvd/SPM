package com.team.lms.admin.service.impl;

import com.team.lms.admin.service.OperationLogService;
import com.team.lms.entity.OperationLog;
import com.team.lms.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Resource
    private OperationLogMapper operationLogMapper;

    @Override
    public void logOperation(String moduleName, String actionName, String operatorName, String resultMessage) {
        OperationLog log = new OperationLog();
        log.setModuleName(moduleName);
        log.setActionName(actionName);
        log.setOperatorName(operatorName);
        log.setResultMessage(resultMessage);
        operationLogMapper.insert(log);
    }

    @Override
    public List<OperationLog> getLogList(String operatorName, LocalDateTime startTime, LocalDateTime endTime, String actionName, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        return operationLogMapper.selectByCondition(operatorName, startTime, endTime, actionName, offset, size);
    }

    @Override
    public Integer getLogCount(String operatorName, LocalDateTime startTime, LocalDateTime endTime, String actionName) {
        return operationLogMapper.countByCondition(operatorName, startTime, endTime, actionName);
    }

    @Override
    public List<OperationLog> getAllLogs() {
        return operationLogMapper.selectAll();
    }
}