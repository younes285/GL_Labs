package com.library.api.controller;

import com.library.api.dto.BookDTO;
import com.library.api.mapper.BookMapper;
import com.library.api.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.URI;

@RestController
@RequestMapping(value = "/books", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Books", description = "Manage the library catalogue")
@ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
public class BookController {
    private final BookService bookService;
    private final BookMapper bookMapper;

    public BookController(BookService bookService, BookMapper bookMapper) {
        this.bookService = bookService;
        this.bookMapper = bookMapper;
    }

    @GetMapping
    @Operation(summary = "List all books")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "List all books - successful response")})
    public List<BookDTO> getAll() {
        return bookService.getAll().stream().map(bookMapper::toDTO).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a book by ID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Get a book by ID - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    public BookDTO getById(@PathVariable Long id) {
        return bookMapper.toDTO(bookService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a book")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Create a book - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")})
    public ResponseEntity<BookDTO> create(@Valid @RequestBody BookDTO book) {
        BookDTO saved = bookMapper.toDTO(bookService.create(bookMapper.toEntity(book)));
        return ResponseEntity.created(URI.create("/books/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a book's catalogue data")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Replace a book's catalogue data - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    public BookDTO update(@PathVariable Long id, @Valid @RequestBody BookDTO updated) {
        return bookMapper.toDTO(bookService.update(id, bookMapper.toEntity(updated)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an unreferenced book")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Delete an unreferenced book - successful response"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")})
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }
}
