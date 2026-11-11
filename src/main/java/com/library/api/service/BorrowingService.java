package com.library.api.service;

import com.library.api.entity.Book;
import com.library.api.entity.Borrowing;
import com.library.api.entity.Member;
import com.library.api.repository.BookRepository;
import com.library.api.repository.BorrowingRepository;
import com.library.api.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BorrowingService {
    private static final BigDecimal DAILY_FINE_RATE = new BigDecimal("0.50");
    private static final BigDecimal ZERO_FINE = new BigDecimal("0.00");
    private static final int DEFAULT_LOAN_DAYS = 14;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BorrowingRepository borrowingRepository;

    public BorrowingService(BookRepository bookRepository, MemberRepository memberRepository,
                            BorrowingRepository borrowingRepository) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.borrowingRepository = borrowingRepository;
    }

    @Transactional
    public Borrowing borrowBook(Long bookId, Long memberId) {
        Book book = findBook(bookId);
        Member member = findMember(memberId);
        if (!book.isAvailable()) {
            throw new IllegalStateException("Book is already borrowed");
        }
        LocalDate today = LocalDate.now();
        book.setAvailable(false);
        return borrowingRepository.save(new Borrowing(null, book, member, today,
                today.plusDays(DEFAULT_LOAN_DAYS), null, ZERO_FINE));
    }

    @Transactional
    public Borrowing returnBook(Long borrowingId) {
        Borrowing borrowing = findBorrowing(borrowingId);
        if (borrowing.getReturnDate() != null) {
            throw new IllegalStateException("Borrowing has already been returned");
        }
        LocalDate today = LocalDate.now();
        borrowing.setReturnDate(today);
        borrowing.getBook().setAvailable(true);
        borrowing.setFineAmount(calculateFine(borrowing.getDueDate(), today));
        return borrowingRepository.save(borrowing);
    }

    @Transactional(readOnly = true)
    public List<Borrowing> getActiveBorrowings(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("Member not found");
        }
        return borrowingRepository.findByMemberIdAndReturnDateIsNullOrderByIdAsc(memberId);
    }

    public BigDecimal calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate));
        return DAILY_FINE_RATE.multiply(BigDecimal.valueOf(overdueDays));
    }

    private Book findBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
    }

    private Borrowing findBorrowing(Long borrowingId) {
        return borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new EntityNotFoundException("Borrowing not found"));
    }
}
