package com.team.lms.entity;

import com.team.lms.common.enums.BorrowRecordStatus;
import com.team.lms.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowRecord extends BaseEntity {
    private User reader;
    private Book book;
    private BorrowRequest borrowRequest;
    private BookCopy bookCopy;
    private BorrowRecordStatus status;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Integer renewalCount;
}
