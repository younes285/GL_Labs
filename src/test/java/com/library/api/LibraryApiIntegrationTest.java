package com.library.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.api.entity.Book;
import com.library.api.entity.Borrowing;
import com.library.api.entity.Member;
import com.library.api.repository.BookRepository;
import com.library.api.repository.BorrowingRepository;
import com.library.api.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LibraryApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private BookRepository books;
    @Autowired private MemberRepository members;
    @Autowired private BorrowingRepository borrowings;

    @Test
    void seededCollectionsContainFiveBooksAndThreeMembers() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
        mvc.perform(get("/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].email").value("maya.haddad@usj.edu.lb"));
    }

    @Test
    void bookCanBeCreatedReadUpdatedAndDeleted() throws Exception {
        Book request = new Book(999L, "9780000000001", "Test Book", "Test Author", "Reference", 2026, true);
        String response = mvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        JsonNode saved = mapper.readTree(response);
        long id = saved.get("id").asLong();
        org.junit.jupiter.api.Assertions.assertNotEquals(999L, id);
        mvc.perform(get("/books/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Test Book"));
        request.setTitle("Updated Book");
        request.setAvailable(false);
        mvc.perform(put("/books/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Updated Book"))
                .andExpect(jsonPath("$.available").value(false));
        assertEquals("Updated Book", books.findById(id).orElseThrow().getTitle());
        mvc.perform(delete("/books/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/books/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void memberCanBeCreatedAndRead() throws Exception {
        Member request = new Member(999L, "Test Member", "test@usj.edu.lb", LocalDate.of(2026, 11, 11), true);
        String response = mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).get("id").asLong();
        org.junit.jupiter.api.Assertions.assertNotEquals(999L, id);
        mvc.perform(get("/members/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@usj.edu.lb"))
                .andExpect(jsonPath("$.membershipDate").value("2026-11-11"));
    }

    @Test
    void missingRecordsReturn404IncludingUpdateAndDelete() throws Exception {
        Book request = new Book(null, "9780000000002", "Missing", "Author", "Reference", 2026, true);
        mvc.perform(get("/books/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/members/999999")).andExpect(status().isNotFound());
        mvc.perform(put("/books/999999").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))).andExpect(status().isNotFound());
        mvc.perform(delete("/books/999999")).andExpect(status().isNotFound());
    }

    @Test
    void invalidInputIsRejectedWithoutSavingRecords() throws Exception {
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(5, books.count());
        assertEquals(3, members.count());
    }

    @Test
    void borrowingEntityPersistsRelationshipsDatesAndDecimalFine() {
        Book book = books.findAll().get(0);
        Member member = members.findAll().get(0);
        LocalDate today = LocalDate.of(2026, 11, 11);
        Borrowing saved = borrowings.saveAndFlush(new Borrowing(null, book, member,
                today, today.plusDays(14), null, new BigDecimal("0.00")));
        assertNotNull(saved.getId());
        Borrowing stored = borrowings.findById(saved.getId()).orElseThrow();
        assertEquals(book.getId(), stored.getBook().getId());
        assertEquals(member.getId(), stored.getMember().getId());
        assertEquals(today.plusDays(14), stored.getDueDate());
        assertEquals(new BigDecimal("0.00"), stored.getFineAmount());
        assertNull(stored.getReturnDate());
    }

    @Test
    void openApiDocumentsBothResources() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/books'].get").exists())
                .andExpect(jsonPath("$.paths['/members'].post").exists());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }
}
