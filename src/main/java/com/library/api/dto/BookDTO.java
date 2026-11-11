package com.library.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookDTO(
        @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY) Long id,
        @NotBlank @Schema(example = "9780132350884") String isbn,
        @NotBlank @Schema(example = "Clean Code") String title,
        @NotBlank @Schema(example = "Robert C. Martin") String author,
        @NotBlank @Schema(example = "Software Engineering") String genre,
        @NotNull @Min(1) @Schema(example = "2008") Integer publishedYear,
        @Schema(example = "true", defaultValue = "true") Boolean available) {
    public BookDTO {
        if (available == null) {
            available = true;
        }
    }
}
