package com.team.lms.entity;

import com.team.lms.common.enums.ShelfStatus;
import com.team.lms.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Book extends BaseEntity {
    private String title;
    private String author;
    private String isbn;
    private String barcode;
    private String publisher;
    private String description;
    private String thumbnailUrl;
    private String publishedDate;
    private Category category;
    private ShelfStatus shelfStatus;
    private String storageLocation;
}
