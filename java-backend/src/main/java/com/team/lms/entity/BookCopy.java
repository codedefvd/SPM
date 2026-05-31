package com.team.lms.entity;

import com.team.lms.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BookCopy extends BaseEntity {
    private Book book;
    private Integer copyNo;
    private String barcode;
    private String storageLocation;
}
