package com.library.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record ReservationDTO(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long bookId,
        @Schema(example = "2") Long memberId,
        @Schema(example = "2026-11-11") LocalDate reservationDate,
        @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "CANCELLED"}) String status) {
}
