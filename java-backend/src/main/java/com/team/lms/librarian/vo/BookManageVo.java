package com.team.lms.librarian.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookManageVo {
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private String barcode;
    private Long categoryId;
    private String categoryName;
    private String publisher;
    private String description;
    private String thumbnailUrl;
    private String publishedDate;
    private Integer totalCopies;
    private Integer availableCopies;
    private String shelfStatus;
    private String storageLocation;
}
