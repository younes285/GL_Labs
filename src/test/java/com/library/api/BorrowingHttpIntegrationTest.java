package com.library.api;

import com.library.api.dto.BookDTO;
import com.library.api.dto.BorrowingDTO;
import com.library.api.dto.MemberDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BorrowingHttpIntegrationTest {
    @Autowired private TestRestTemplate rest;

    @Test
    void createBookMemberAndBorrowingMakesBookUnavailable() {
        BookDTO book = createBook("9780000000101");
        MemberDTO member = createMember("flow1@usj.edu.lb");

        ResponseEntity<BorrowingDTO> borrowing = borrow(book.id(), member.id());
        ResponseEntity<BookDTO> storedBook = rest.getForEntity("/books/{id}", BookDTO.class, book.id());

        assertEquals(HttpStatus.CREATED, borrowing.getStatusCode());
        assertNotNull(borrowing.getBody());
        assertEquals(book.id(), borrowing.getBody().bookId());
        assertEquals(member.id(), borrowing.getBody().memberId());
        assertNotNull(storedBook.getBody());
        assertFalse(storedBook.getBody().available());
    }

    @Test
    void fullBorrowingCycleRestoresBookAvailability() {
        BookDTO book = createBook("9780000000102");
        MemberDTO member = createMember("flow2@usj.edu.lb");
        BorrowingDTO borrowing = borrow(book.id(), member.id()).getBody();
        assertNotNull(borrowing);

        ResponseEntity<BorrowingDTO> returned = rest.exchange("/borrowings/{id}/return",
                org.springframework.http.HttpMethod.PUT, null, BorrowingDTO.class, borrowing.id());
        ResponseEntity<BookDTO> storedBook = rest.getForEntity("/books/{id}", BookDTO.class, book.id());

        assertEquals(HttpStatus.OK, returned.getStatusCode());
        assertNotNull(returned.getBody());
        assertNotNull(returned.getBody().returnDate());
        assertNotNull(storedBook.getBody());
        assertTrue(storedBook.getBody().available());
    }

    @Test
    void borrowingSameBookTwiceReturnsConflict() {
        BookDTO book = createBook("9780000000103");
        MemberDTO member = createMember("flow3@usj.edu.lb");

        assertEquals(HttpStatus.CREATED, borrow(book.id(), member.id()).getStatusCode());
        ResponseEntity<String> duplicate = rest.postForEntity("/borrowings",
                Map.of("bookId", book.id(), "memberId", member.id()), String.class);

        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertNotNull(duplicate.getBody());
        assertTrue(duplicate.getBody().contains("Book is already borrowed"));
    }

    private BookDTO createBook(String isbn) {
        BookDTO request = new BookDTO(null, isbn, "Integration Book", "Test Author",
                "Testing", 2026, true);
        ResponseEntity<BookDTO> response = rest.postForEntity("/books", request, BookDTO.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private MemberDTO createMember(String email) {
        MemberDTO request = new MemberDTO(null, "Integration Member", email, LocalDate.now(), true);
        ResponseEntity<MemberDTO> response = rest.postForEntity("/members", request, MemberDTO.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private ResponseEntity<BorrowingDTO> borrow(Long bookId, Long memberId) {
        return rest.postForEntity("/borrowings", Map.of("bookId", bookId, "memberId", memberId),
                BorrowingDTO.class);
    }
}
