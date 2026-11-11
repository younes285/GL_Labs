package com.library.api.controller;

import com.library.api.dto.BorrowingDTO;
import com.library.api.mapper.BorrowingMapper;
import com.library.api.service.BorrowingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import java.util.List;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Borrowings", description = "Borrow books, return them and view active loans")
@ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
public class BorrowingController {
    private final BorrowingService borrowingService;
    private final BorrowingMapper borrowingMapper;

    public BorrowingController(BorrowingService borrowingService, BorrowingMapper borrowingMapper) {
        this.borrowingService = borrowingService;
        this.borrowingMapper = borrowingMapper;
    }

    @PostMapping("/borrowings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Borrow an available book for 14 days")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Borrow an available book for 14 days - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")})
    public BorrowingDTO borrow(@Valid @RequestBody BorrowRequest request) {
        return borrowingMapper.toDTO(borrowingService.borrowBook(request.bookId(), request.memberId()));
    }

    @PutMapping("/borrowings/{id}/return")
    @Operation(summary = "Return a loan and calculate its overdue fine")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Return a loan and calculate its overdue fine - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")})
    public BorrowingDTO returnBook(@PathVariable Long id) {
        return borrowingMapper.toDTO(borrowingService.returnBook(id));
    }

    @GetMapping("/members/{id}/borrowings")
    @Operation(summary = "List a member's active loans")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "List a member's active loans - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    public List<BorrowingDTO> activeBorrowings(@PathVariable Long id) {
        return borrowingService.getActiveBorrowings(id).stream().map(borrowingMapper::toDTO).toList();
    }

    public record BorrowRequest(
            @NotNull @Positive @Schema(example = "1") Long bookId,
            @NotNull @Positive @Schema(example = "2") Long memberId) {
    }
}
