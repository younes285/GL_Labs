package com.library.api.repository;

import com.library.api.entity.Reservation;
import com.library.api.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, ReservationStatus status);

    List<Reservation> findByMemberIdAndStatusOrderByIdAsc(Long memberId, ReservationStatus status);
}
