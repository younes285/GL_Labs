package com.library.api.controller;

import com.library.api.entity.Borrowing;
import com.library.api.service.BorrowingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BorrowingController {
    private final BorrowingService borrowingService;

    public BorrowingController(BorrowingService borrowingService) {
        this.borrowingService = borrowingService;
    }

    @PostMapping("/borrowings")
    @ResponseStatus(HttpStatus.CREATED)
    public Borrowing borrow(@Valid @RequestBody BorrowRequest request) {
        return borrowingService.borrowBook(request.bookId(), request.memberId());
    }

    @PutMapping("/borrowings/{id}/return")
    public Borrowing returnBook(@PathVariable Long id) {
        return borrowingService.returnBook(id);
    }

    @GetMapping("/members/{id}/borrowings")
    public List<Borrowing> activeBorrowings(@PathVariable Long id) {
        return borrowingService.getActiveBorrowings(id);
    }

    public record BorrowRequest(@NotNull @Positive Long bookId, @NotNull @Positive Long memberId) {
    }
}
