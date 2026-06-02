package com.team.lms.librarian.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookCopyLocationUpdateRequest {
    @NotBlank(message = "storage location is required")
    private String storageLocation;
}
