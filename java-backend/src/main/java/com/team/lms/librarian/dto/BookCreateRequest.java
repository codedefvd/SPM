package com.team.lms.librarian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class BookCreateRequest {
    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "author is required")
    private String author;

    @NotBlank(message = "isbn is required")
    private String isbn;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    private String publisher;
    private String description;
    private String thumbnailUrl;
    private String publishedDate;
    private String storageLocation;

    @NotNull(message = "totalCopies is required")
    @PositiveOrZero(message = "totalCopies must be >= 0")
    private Integer totalCopies;

    @NotNull(message = "availableCopies is required")
    @PositiveOrZero(message = "availableCopies must be >= 0")
    private Integer availableCopies;

    @NotBlank(message = "shelfStatus is required")
    private String shelfStatus;
}
