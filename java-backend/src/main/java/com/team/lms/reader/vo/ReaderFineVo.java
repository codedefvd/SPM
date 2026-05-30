package com.team.lms.reader.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReaderFineVo {
    private Long fineId;
    private Long recordId;
    private Long bookId;
    private String bookTitle;
    private String copyBarcode;
    private BigDecimal amount;
    private String status;
    private Long overdueDays;
    private String dueDate;
    private String returnDate;
}
