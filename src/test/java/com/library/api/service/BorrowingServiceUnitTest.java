package com.library.api.service;

import com.library.api.entity.Book;
import com.library.api.entity.Borrowing;
import com.library.api.entity.Member;
import com.library.api.repository.BookRepository;
import com.library.api.repository.BorrowingRepository;
import com.library.api.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceUnitTest {
    @Mock private BookRepository bookRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private BorrowingRepository borrowingRepository;
    @InjectMocks private BorrowingService service;

    @Test
    void borrowBook_availableBookCreatesFourteenDayLoan() {
        Book book = book(true);
        Member member = member();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Borrowing result = service.borrowBook(1L, 2L);

        assertSame(book, result.getBook());
        assertSame(member, result.getMember());
        assertFalse(book.isAvailable());
        assertEquals(result.getBorrowDate().plusDays(14), result.getDueDate());
        assertEquals(new BigDecimal("0.00"), result.getFineAmount());
        verify(borrowingRepository).save(result);
    }

    @Test
    void borrowBook_missingBookThrowsAndStopsBeforeMemberLookup() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException error = assertThrows(EntityNotFoundException.class,
                () -> service.borrowBook(1L, 2L));

        assertEquals("Book not found", error.getMessage());
        verify(memberRepository, never()).findById(any());
        verify(borrowingRepository, never()).save(any());
    }

    @Test
    void borrowBook_missingMemberThrowsWithoutChangingAvailability() {
        Book book = book(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(memberRepository.findById(2L)).thenReturn(Optional.empty());

        EntityNotFoundException error = assertThrows(EntityNotFoundException.class,
                () -> service.borrowBook(1L, 2L));

        assertEquals("Member not found", error.getMessage());
        assertTrue(book.isAvailable());
        verify(borrowingRepository, never()).save(any());
    }

    @Test
    void borrowBook_unavailableBookThrowsWithoutSaving() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(false)));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member()));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.borrowBook(1L, 2L));

        assertEquals("Book is already borrowed", error.getMessage());
        verify(borrowingRepository, never()).save(any());
    }

    @Test
    void returnBook_onTimeRestoresAvailabilityWithZeroFine() {
        LocalDate today = LocalDate.now();
        Borrowing borrowing = borrowing(today, null);
        when(borrowingRepository.findById(3L)).thenReturn(Optional.of(borrowing));
        when(borrowingRepository.save(borrowing)).thenReturn(borrowing);

        Borrowing result = service.returnBook(3L);

        assertEquals(today, result.getReturnDate());
        assertEquals(new BigDecimal("0.00"), result.getFineAmount());
        assertTrue(result.getBook().isAvailable());
        verify(borrowingRepository).save(borrowing);
    }

    @Test
    void returnBook_threeDaysLateChargesOneEuroFifty() {
        LocalDate today = LocalDate.now();
        Borrowing borrowing = borrowing(today.minusDays(3), null);
        when(borrowingRepository.findById(3L)).thenReturn(Optional.of(borrowing));
        when(borrowingRepository.save(borrowing)).thenReturn(borrowing);

        Borrowing result = service.returnBook(3L);

        assertEquals(new BigDecimal("1.50"), result.getFineAmount());
        assertEquals(today, result.getReturnDate());
        assertTrue(result.getBook().isAvailable());
    }

    @Test
    void returnBook_missingBorrowingThrowsWithoutSaving() {
        when(borrowingRepository.findById(3L)).thenReturn(Optional.empty());

        EntityNotFoundException error = assertThrows(EntityNotFoundException.class,
                () -> service.returnBook(3L));

        assertEquals("Borrowing not found", error.getMessage());
        verify(borrowingRepository, never()).save(any());
    }

    @Test
    void returnBook_alreadyReturnedThrowsWithoutSaving() {
        Borrowing borrowing = borrowing(LocalDate.now(), LocalDate.now());
        when(borrowingRepository.findById(3L)).thenReturn(Optional.of(borrowing));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.returnBook(3L));

        assertEquals("Borrowing has already been returned", error.getMessage());
        verify(borrowingRepository, never()).save(any());
    }

    @Test
    void getActiveBorrowings_existingMemberReturnsRepositoryResult() {
        List<Borrowing> expected = List.of(borrowing(LocalDate.now(), null));
        when(memberRepository.existsById(2L)).thenReturn(true);
        when(borrowingRepository.findByMemberIdAndReturnDateIsNullOrderByIdAsc(2L)).thenReturn(expected);

        List<Borrowing> result = service.getActiveBorrowings(2L);

        assertSame(expected, result);
        verify(borrowingRepository).findByMemberIdAndReturnDateIsNullOrderByIdAsc(2L);
    }

    @Test
    void getActiveBorrowings_missingMemberThrowsWithoutQueryingLoans() {
        when(memberRepository.existsById(2L)).thenReturn(false);

        EntityNotFoundException error = assertThrows(EntityNotFoundException.class,
                () -> service.getActiveBorrowings(2L));

        assertEquals("Member not found", error.getMessage());
        verify(borrowingRepository, never()).findByMemberIdAndReturnDateIsNullOrderByIdAsc(any());
    }

    @Test
    void calculateFine_returnedBeforeDueDateIsZero() {
        assertEquals(new BigDecimal("0.00"), service.calculateFine(date(), date().minusDays(2)));
    }

    @Test
    void calculateFine_returnedOnDueDateIsZero() {
        assertEquals(new BigDecimal("0.00"), service.calculateFine(date(), date()));
    }

    @Test
    void calculateFine_oneDayLateIsFiftyCents() {
        assertEquals(new BigDecimal("0.50"), service.calculateFine(date(), date().plusDays(1)));
    }

    @Test
    void calculateFine_sevenDaysLateIsThreeEurosFifty() {
        assertEquals(new BigDecimal("3.50"), service.calculateFine(date(), date().plusDays(7)));
    }

    private Book book(boolean available) {
        return new Book(1L, "9780132350884", "Clean Code", "Robert C. Martin",
                "Software Engineering", 2008, available);
    }

    private Member member() {
        return new Member(2L, "Maya Haddad", "maya@usj.edu.lb", date(), true);
    }

    private Borrowing borrowing(LocalDate dueDate, LocalDate returnDate) {
        return new Borrowing(3L, book(false), member(), dueDate.minusDays(14), dueDate,
                returnDate, new BigDecimal("0.00"));
    }

    private LocalDate date() {
        return LocalDate.of(2026, 11, 11);
    }
}
