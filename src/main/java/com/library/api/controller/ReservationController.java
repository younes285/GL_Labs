package com.library.api.controller;

import com.library.api.dto.ReservationDTO;
import com.library.api.mapper.ReservationMapper;
import com.library.api.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import java.util.List;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Reservations", description = "Reserve unavailable books and cancel recorded requests")
@ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;

    public ReservationController(ReservationService reservationService, ReservationMapper reservationMapper) {
        this.reservationService = reservationService;
        this.reservationMapper = reservationMapper;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserve an unavailable book for an active member")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Reserve an unavailable book for an active member - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")})
    public ReservationDTO reserve(@Valid @RequestBody ReservationRequest request) {
        return reservationMapper.toDTO(reservationService.reserveBook(request.bookId(), request.memberId()));
    }

    @GetMapping("/members/{id}/reservations")
    @Operation(summary = "List a member's ACTIVE reservations in ID order")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "List a member's ACTIVE reservations in ID order - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    public List<ReservationDTO> activeReservations(@PathVariable Long id) {
        return reservationService.getActiveReservations(id).stream().map(reservationMapper::toDTO).toList();
    }

    @DeleteMapping("/reservations/{id}")
    @Operation(summary = "Cancel a reservation while retaining its history")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Cancel a reservation while retaining its history - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    public ReservationDTO cancel(@PathVariable Long id) {
        return reservationMapper.toDTO(reservationService.cancelReservation(id));
    }

    public record ReservationRequest(
            @NotNull @Positive @Schema(example = "1") Long bookId,
            @NotNull @Positive @Schema(example = "2") Long memberId) {
    }
}
