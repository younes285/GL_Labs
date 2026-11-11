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

@Service
public class BorrowingService {
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
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
        if (!book.isAvailable()) {
            throw new IllegalStateException("Book is already borrowed");
        }
        LocalDate today = LocalDate.now();
        book.setAvailable(false);
        return borrowingRepository.save(new Borrowing(null, book, member, today,
                today.plusDays(14), null, new BigDecimal("0.00")));
    }
}
