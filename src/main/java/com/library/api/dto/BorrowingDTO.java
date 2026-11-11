package com.library.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BorrowingDTO(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long bookId,
        @Schema(example = "2") Long memberId,
        @Schema(example = "2026-11-11") LocalDate borrowDate,
        @Schema(example = "2026-11-25") LocalDate dueDate,
        @Schema(example = "2026-11-26", nullable = true) LocalDate returnDate,
        @Schema(example = "0.50") BigDecimal fineAmount) {
}
