package com.library.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        @Schema(example = "Book not found") String error,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(example = "2026-11-11T14:00:00Z") Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @Schema(example = "{\"bookId\":\"must not be null\"}") Map<String, String> fieldErrors) {
    public ErrorResponse(String error) {
        this(error, Instant.now(), null);
    }
}
