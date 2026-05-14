package com.team.lms.librarian.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FineUpdateRequest {
    private String status;

    @PositiveOrZero(message = "amount must be >= 0")
    private BigDecimal amount;
}
