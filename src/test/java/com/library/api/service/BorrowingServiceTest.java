package com.library.api.service;

import com.library.api.entity.Book;
import com.library.api.entity.Borrowing;
import com.library.api.entity.Member;
import com.library.api.repository.BookRepository;
import com.library.api.repository.BorrowingRepository;
import com.library.api.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BorrowingServiceTest {
    @Autowired private BorrowingService borrowingService;
    @Autowired private BookRepository bookRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private BorrowingRepository borrowingRepository;

    @Test
    void borrowBook_shouldMarkBookAsUnavailable() {
        Book book = bookRepository.save(new Book(null, "978-0-13-468599-1",
                "Clean Code", "Robert C. Martin", "Engineering", 2008, true));
        Member member = memberRepository.save(new Member(null, "Alice",
                "alice@usj.edu.lb", LocalDate.now(), true));
        LocalDate today = LocalDate.now();

        Borrowing result = borrowingService.borrowBook(book.getId(), member.getId());

        assertNotNull(result.getId());
        assertFalse(bookRepository.findById(book.getId()).orElseThrow().isAvailable());
        assertEquals(today, result.getBorrowDate());
        assertEquals(today.plusDays(14), result.getDueDate());
        assertEquals(member.getId(), result.getMember().getId());
        assertNull(result.getReturnDate());
    }

    @Test
    void borrowBook_rejectsUnavailableBookWithoutCreatingAnotherLoan() {
        Book book = bookRepository.findAll().get(0);
        Member member = memberRepository.findAll().get(0);
        book.setAvailable(false);
        bookRepository.saveAndFlush(book);
        long previousCount = borrowingRepository.count();

        assertThrows(IllegalStateException.class,
                () -> borrowingService.borrowBook(book.getId(), member.getId()));
        assertEquals(previousCount, borrowingRepository.count());
    }

    @Test
    void returnBook_restoresAvailabilityAndRecordsReturnDate() {
        Borrowing loan = borrowingService.borrowBook(bookRepository.findAll().get(0).getId(),
                memberRepository.findAll().get(0).getId());

        Borrowing result = borrowingService.returnBook(loan.getId());

        assertEquals(LocalDate.now(), result.getReturnDate());
        assertTrue(bookRepository.findById(result.getBook().getId()).orElseThrow().isAvailable());
        assertEquals(new BigDecimal("0.00"), result.getFineAmount());
    }

    @Test
    void returnBook_chargesFiftyCentsForEachOverdueDay() {
        Book book = bookRepository.findAll().get(0);
        Member member = memberRepository.findAll().get(0);
        LocalDate today = LocalDate.now();
        book.setAvailable(false);
        Borrowing loan = borrowingRepository.saveAndFlush(new Borrowing(null, book, member,
                today.minusDays(17), today.minusDays(3), null, new BigDecimal("0.00")));

        Borrowing result = borrowingService.returnBook(loan.getId());

        assertEquals(new BigDecimal("1.50"), result.getFineAmount());
        assertEquals(today, result.getReturnDate());
        assertTrue(result.getBook().isAvailable());
    }
}
