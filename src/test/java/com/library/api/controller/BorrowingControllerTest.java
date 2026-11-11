package com.library.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.api.repository.BorrowingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BorrowingControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private BorrowingRepository borrowingRepository;

    @Test
    void borrowingAndReturnCycleUpdatesBookAndActiveLoans() throws Exception {
        JsonNode loan = borrow(1, 1);
        long id = loan.get("id").asLong();
        assertEquals(LocalDate.now().plusDays(14).toString(), loan.get("dueDate").asText());
        mvc.perform(get("/books/1")).andExpect(jsonPath("$.available").value(false));
        mvc.perform(get("/members/1/borrowings"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(put("/borrowings/{id}/return", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.fineAmount").value(0.0));
        mvc.perform(get("/books/1")).andExpect(jsonPath("$.available").value(true));
        mvc.perform(get("/members/1/borrowings"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void secondBorrowReturns409WithoutCreatingAnotherLoan() throws Exception {
        borrow(1, 1);

        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Book is already borrowed"));
        assertEquals(1, borrowingRepository.count());
        mvc.perform(get("/books/1")).andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void repeatedReturnCannotMakeAnAlreadyReborrowedBookAvailable() throws Exception {
        long oldId = borrow(1, 1).get("id").asLong();
        mvc.perform(put("/borrowings/{id}/return", oldId)).andExpect(status().isOk());
        JsonNode secondLoan = borrow(1, 2);

        mvc.perform(put("/borrowings/{id}/return", oldId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Borrowing has already been returned"));
        mvc.perform(get("/books/1")).andExpect(jsonPath("$.available").value(false));
        mvc.perform(get("/members/2/borrowings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(secondLoan.get("id").asLong()));
    }

    @Test
    void missingResourcesReturn404WithoutChangingBookAvailability() throws Exception {
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":999999,\"memberId\":1}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Book not found"));
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":999999}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Member not found"));
        mvc.perform(put("/borrowings/999999/return")).andExpect(status().isNotFound());
        mvc.perform(get("/members/999999/borrowings")).andExpect(status().isNotFound());
        mvc.perform(get("/books/1")).andExpect(jsonPath("$.available").value(true));
        assertEquals(0, borrowingRepository.count());
    }

    @Test
    void missingOrNonpositiveRequestIdsReturn400() throws Exception {
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":0,\"memberId\":-1}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, borrowingRepository.count());
    }

    private JsonNode borrow(long bookId, long memberId) throws Exception {
        String response = mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new BorrowingController.BorrowRequest(bookId, memberId))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response);
    }
}
