package com.library.api.controller;

import com.library.api.entity.Borrowing;
import com.library.api.service.BorrowingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    public record BorrowRequest(@NotNull @Positive Long bookId, @NotNull @Positive Long memberId) {
    }
}
