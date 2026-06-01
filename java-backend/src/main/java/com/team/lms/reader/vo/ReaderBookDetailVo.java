package com.team.lms.reader.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReaderBookDetailVo {
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String thumbnailUrl;
    private String publishedDate;
    private String description;
    private String categoryName;
    private Integer totalCopies;
    private Integer availableCopies;
    private String shelfStatus;
    private String storageLocation;
    private Boolean favorite;
    private Double averageRating;
    private Integer reviewCount;
    private Boolean canBorrow;
    private Boolean canReserve;
    private Boolean canReview;
    private List<ReaderBookReviewVo> reviews;
}
