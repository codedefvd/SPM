package com.team.lms.librarian.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FineManageVo {
    private Long fineId;
    private Long recordId;
    private String bookTitle;
    private String copyBarcode;
    private Long readerId;
    private String readerUsername;
    private String dueDate;
    private Long overdueDays;
    private BigDecimal amount;
    private String status;
}
