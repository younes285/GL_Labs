package com.library.api.service;

import com.library.api.entity.Book;
import com.library.api.entity.Member;
import com.library.api.entity.Reservation;
import com.library.api.entity.ReservationStatus;
import com.library.api.repository.BookRepository;
import com.library.api.repository.MemberRepository;
import com.library.api.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(BookRepository bookRepository, MemberRepository memberRepository,
                              ReservationRepository reservationRepository) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Reservation reserveBook(Long bookId, Long memberId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
        if (book.isAvailable()) {
            throw new IllegalStateException("Book is available for direct borrowing.");
        }
        if (!member.isActive()) {
            throw new IllegalStateException("Member is inactive.");
        }
        if (reservationRepository.existsByBookIdAndMemberIdAndStatus(bookId, memberId, ReservationStatus.ACTIVE)) {
            throw new IllegalStateException("An active reservation already exists.");
        }
        return reservationRepository.save(new Reservation(null, book, member,
                LocalDate.now(), ReservationStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public List<Reservation> getActiveReservations(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("Member not found");
        }
        return reservationRepository.findByMemberIdAndStatusOrderByIdAsc(memberId, ReservationStatus.ACTIVE);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }
}
