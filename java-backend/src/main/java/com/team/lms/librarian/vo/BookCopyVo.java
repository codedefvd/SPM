package com.team.lms.librarian.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookCopyVo {
    private Long copyId;
    private Integer copyNo;
    private String barcode;
    private String storageLocation;
}
