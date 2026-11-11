package com.library.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MemberDTO(
        @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY) Long id,
        @NotBlank @Schema(example = "Maya Haddad") String name,
        @NotBlank @Email @Schema(example = "maya.haddad@usj.edu.lb") String email,
        @Schema(example = "2026-09-01", description = "Defaults to today's server-local date") LocalDate membershipDate,
        @Schema(example = "true", defaultValue = "true") Boolean active) {
    public MemberDTO {
        if (membershipDate == null) {
            membershipDate = LocalDate.now();
        }
        if (active == null) {
            active = true;
        }
    }
}
