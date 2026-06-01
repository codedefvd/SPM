package com.team.lms.reader.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReaderBorrowRecordVo {
    private Long recordId;
    private Long bookId;
    private String bookTitle;
    private String copyBarcode;
    private String status;
    private String borrowDate;
    private String dueDate;
    private String returnDate;
    private BigDecimal fineAmount;
    private Long overdueDays;
    private Boolean canRequestReturn;
    private Boolean canRenew;
    private Integer renewalCount;
    private Integer maxRenewals;
    private String storageLocation;
    private String message;
}
