package com.library.api.service;

import com.library.api.entity.Book;
import com.library.api.entity.Member;
import com.library.api.entity.Reservation;
import com.library.api.entity.ReservationStatus;
import com.library.api.repository.BookRepository;
import com.library.api.repository.BorrowingRepository;
import com.library.api.repository.MemberRepository;
import com.library.api.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReservationServiceTest {
    @Autowired private ReservationService service;
    @Autowired private ReservationRepository reservations;
    @Autowired private BookRepository books;
    @Autowired private MemberRepository members;
    @Autowired private BorrowingRepository borrowings;

    @Test
    void ac1_availableBookIsRejectedWithoutSaving() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.reserveBook(1L, 1L));
        assertEquals("Book is available for direct borrowing.", error.getMessage());
        assertEquals(0, reservations.count());
        assertTrue(books.findById(1L).orElseThrow().isAvailable());
    }

    @Test
    void ac2_creationSetsServerFieldsWithoutChangingExistingDomainData() {
        Book book = unavailableBook(1L);
        Member member = members.findById(1L).orElseThrow();
        String originalEmail = member.getEmail();
        long previousBorrowings = borrowings.count();
        LocalDate today = LocalDate.now();

        Reservation reservation = service.reserveBook(book.getId(), member.getId());
        reservations.flush();

        assertNotNull(reservation.getId());
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
        assertEquals(today, reservation.getReservationDate());
        assertEquals(book.getId(), reservation.getBook().getId());
        assertEquals(member.getId(), reservation.getMember().getId());
        assertFalse(books.findById(book.getId()).orElseThrow().isAvailable());
        assertEquals(originalEmail, members.findById(member.getId()).orElseThrow().getEmail());
        assertEquals(previousBorrowings, borrowings.count());
    }

    @Test
    void ac2_duplicatePairIsRejectedButAnotherMemberCanReserve() {
        unavailableBook(1L);
        service.reserveBook(1L, 1L);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.reserveBook(1L, 1L));
        assertEquals("An active reservation already exists.", error.getMessage());
        service.reserveBook(1L, 2L);
        assertEquals(2, reservations.count());
    }

    @Test
    void ac3_listFiltersMemberAndStatusAndOrdersById() {
        Book book = unavailableBook(1L);
        Member member = members.findById(1L).orElseThrow();
        Member other = members.findById(2L).orElseThrow();
        Reservation first = save(book, member, ReservationStatus.ACTIVE);
        save(book, member, ReservationStatus.CANCELLED);
        save(book, other, ReservationStatus.ACTIVE);
        Reservation last = save(book, member, ReservationStatus.ACTIVE);

        List<Reservation> result = service.getActiveReservations(member.getId());

        assertEquals(List.of(first.getId(), last.getId()), result.stream().map(Reservation::getId).toList());
        assertTrue(result.stream().allMatch(r -> r.getStatus() == ReservationStatus.ACTIVE));
        assertTrue(service.getActiveReservations(3L).isEmpty());
    }

    @Test
    void ac4_cancelIsIdempotentRetainsHistoryAndPermitsReservingAgain() {
        unavailableBook(1L);
        Reservation original = service.reserveBook(1L, 1L);

        assertEquals(ReservationStatus.CANCELLED, service.cancelReservation(original.getId()).getStatus());
        assertEquals(ReservationStatus.CANCELLED, service.cancelReservation(original.getId()).getStatus());
        assertTrue(service.getActiveReservations(1L).isEmpty());
        assertEquals(1, reservations.count());
        assertEquals(ReservationStatus.CANCELLED, reservations.findById(original.getId()).orElseThrow().getStatus());
        assertFalse(books.findById(1L).orElseThrow().isAvailable());
        Reservation replacement = service.reserveBook(1L, 1L);
        assertNotEquals(original.getId(), replacement.getId());
        assertEquals(2, reservations.count());
    }

    @Test
    void ac5_missingResourcesUseSpecifiedLookupPrecedence() {
        assertEquals("Book not found", assertThrows(EntityNotFoundException.class,
                () -> service.reserveBook(999999L, 999999L)).getMessage());
        assertEquals("Member not found", assertThrows(EntityNotFoundException.class,
                () -> service.reserveBook(1L, 999999L)).getMessage());
        assertEquals("Member not found", assertThrows(EntityNotFoundException.class,
                () -> service.getActiveReservations(999999L)).getMessage());
        assertEquals("Reservation not found", assertThrows(EntityNotFoundException.class,
                () -> service.cancelReservation(999999L)).getMessage());
        assertEquals(0, reservations.count());
    }

    @Test
    void ac5_inactiveMemberCannotReserve() {
        unavailableBook(1L);
        Member member = members.findById(1L).orElseThrow();
        member.setActive(false);
        members.saveAndFlush(member);

        assertEquals("Member is inactive.", assertThrows(IllegalStateException.class,
                () -> service.reserveBook(1L, 1L)).getMessage());
        assertEquals(0, reservations.count());
        assertFalse(books.findById(1L).orElseThrow().isAvailable());
    }

    @Test
    void br6_returningBookDoesNotAutomaticallyFulfilOrCancelReservation() {
        Book book = unavailableBook(1L);
        Reservation reservation = service.reserveBook(1L, 1L);
        book.setAvailable(true);
        books.saveAndFlush(book);

        assertEquals(List.of(reservation.getId()),
                service.getActiveReservations(1L).stream().map(Reservation::getId).toList());
        service.cancelReservation(reservation.getId());
        assertTrue(books.findById(1L).orElseThrow().isAvailable());
    }

    private Book unavailableBook(Long id) {
        Book book = books.findById(id).orElseThrow();
        book.setAvailable(false);
        return books.saveAndFlush(book);
    }

    private Reservation save(Book book, Member member, ReservationStatus status) {
        return reservations.saveAndFlush(new Reservation(null, book, member, LocalDate.now(), status));
    }
}
