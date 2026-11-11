package com.library.api.controller;

import com.library.api.entity.Reservation;
import com.library.api.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Reservation reserve(@Valid @RequestBody ReservationRequest request) {
        return reservationService.reserveBook(request.bookId(), request.memberId());
    }

    @GetMapping("/members/{id}/reservations")
    public List<Reservation> activeReservations(@PathVariable Long id) {
        return reservationService.getActiveReservations(id);
    }

    @DeleteMapping("/reservations/{id}")
    public Reservation cancel(@PathVariable Long id) {
        return reservationService.cancelReservation(id);
    }

    public record ReservationRequest(@NotNull @Positive Long bookId, @NotNull @Positive Long memberId) {
    }
}
