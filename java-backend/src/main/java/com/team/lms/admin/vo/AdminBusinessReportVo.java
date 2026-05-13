package com.team.lms.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminBusinessReportVo {
    private Integer totalReaders;
    private Integer totalLibrarians;
    private Integer totalAdmins;
    private Integer totalBorrowRequests;
    private Integer pendingBorrowRequests;
    private Integer overdueBorrows;
    private Integer activeReservations;
    private Integer unavailableBooks;
    private Integer totalInventoryCopies;
    private Integer availableInventoryCopies;
    private BigDecimal unpaidFineAmount;
    private BigDecimal paidFineAmount;
    private List<AdminMetricItemVo> borrowStatusBreakdown;
    private List<AdminMetricItemVo> requestStatusBreakdown;
    private List<AdminMetricItemVo> reservationStatusBreakdown;
}
