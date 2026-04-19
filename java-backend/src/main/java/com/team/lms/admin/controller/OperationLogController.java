package com.team.lms.admin.controller;

import com.team.lms.admin.dto.LogQueryDTO;
import com.team.lms.admin.service.OperationLogService;
import com.team.lms.common.api.ApiResponse;
import com.team.lms.entity.OperationLog;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    @PostMapping("/list")
    public ApiResponse<?> getLogList(@RequestBody LogQueryDTO queryDTO) {
        List<OperationLog> logs = operationLogService.getLogList(
                queryDTO.getOperatorName(),
                queryDTO.getStartTime(),
                queryDTO.getEndTime(),
                queryDTO.getActionName(),
                queryDTO.getPage(),
                queryDTO.getSize()
        );
        Integer count = operationLogService.getLogCount(
                queryDTO.getOperatorName(),
                queryDTO.getStartTime(),
                queryDTO.getEndTime(),
                queryDTO.getActionName()
        );
        return ApiResponse.success(new LogListResponse(logs, count));
    }

    @GetMapping("/export")
    public void exportLogs(
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(required = false) String actionName,
            HttpServletResponse response
    ) throws IOException {
        List<OperationLog> logs = operationLogService.getLogList(operatorName, startTime, endTime, actionName, 1, Integer.MAX_VALUE);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("操作日志");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"操作人", "操作时间", "操作类型", "操作对象", "操作结果"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        int rowIndex = 1;
        for (OperationLog log : logs) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(log.getOperatorName());
            row.createCell(1).setCellValue(log.getCreatedAt().toString());
            row.createCell(2).setCellValue(log.getActionName());
            row.createCell(3).setCellValue(log.getModuleName());
            row.createCell(4).setCellValue(log.getResultMessage());
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("操作日志.xlsx", "UTF-8"));

        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();
    }

    private static class LogListResponse {
        private List<OperationLog> list;
        private Integer total;

        public LogListResponse(List<OperationLog> list, Integer total) {
            this.list = list;
            this.total = total;
        }

        public List<OperationLog> getList() {
            return list;
        }

        public void setList(List<OperationLog> list) {
            this.list = list;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }
}