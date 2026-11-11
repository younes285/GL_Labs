package com.library.api.mapper;

import com.library.api.dto.ReservationDTO;
import com.library.api.entity.Reservation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReservationMapper {
    public ReservationDTO toDTO(Reservation reservation) {
        return new ReservationDTO(reservation.getId(), reservation.getBook().getId(), reservation.getMember().getId(),
                reservation.getReservationDate(), reservation.getStatus().name());
    }

    public List<ReservationDTO> toDTOs(List<Reservation> reservations) {
        return reservations.stream().map(this::toDTO).toList();
    }
}
