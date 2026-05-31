package com.team.lms.librarian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookUpdateRequest {
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
}
