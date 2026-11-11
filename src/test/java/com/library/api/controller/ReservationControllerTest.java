package com.library.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.api.entity.Book;
import com.library.api.entity.Member;
import com.library.api.entity.ReservationStatus;
import com.library.api.repository.BookRepository;
import com.library.api.repository.MemberRepository;
import com.library.api.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ReservationRepository reservations;
    @Autowired private BookRepository books;
    @Autowired private MemberRepository members;

    @Test
    void ac1_availableBookReturns409WithExactMessage() throws Exception {
        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Book is available for direct borrowing."));
        assertEquals(0, reservations.count());
    }

    @Test
    void ac2_createReturns201AndDuplicateReturns409() throws Exception {
        unavailableBook(1L);
        JsonNode saved = reserve(1, 1);
        assertTrue(saved.get("id").asLong() > 0);
        assertEquals("ACTIVE", saved.get("status").asText());
        assertEquals(LocalDate.now().toString(), saved.get("reservationDate").asText());
        assertEquals(1, saved.get("book").get("id").asLong());
        assertEquals(1, saved.get("member").get("id").asLong());
        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An active reservation already exists."));
        reserve(1, 2);
        assertEquals(2, reservations.count());
        assertFalse(books.findById(1L).orElseThrow().isAvailable());
    }

    @Test
    void ac3_listExcludesCancelledAndOtherMemberReservations() throws Exception {
        unavailableBook(1L);
        unavailableBook(2L);
        long activeId = reserve(1, 1).get("id").asLong();
        long cancelledId = reserve(2, 1).get("id").asLong();
        reserve(1, 2);
        mvc.perform(delete("/reservations/{id}", cancelledId)).andExpect(status().isOk());

        mvc.perform(get("/members/1/reservations"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activeId))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        mvc.perform(get("/members/3/reservations"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ac4_cancelPreservesRecordAndCanBeRepeated() throws Exception {
        unavailableBook(1L);
        long id = reserve(1, 1).get("id").asLong();
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete("/reservations/{id}", id))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        }
        assertEquals(ReservationStatus.CANCELLED, reservations.findById(id).orElseThrow().getStatus());
        assertEquals(1, reservations.count());
        mvc.perform(get("/members/1/reservations"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertNotEquals(id, reserve(1, 1).get("id").asLong());
        assertFalse(books.findById(1L).orElseThrow().isAvailable());
    }

    @Test
    void ac5_missingResourcesReturn404WithSpecifiedPrecedence() throws Exception {
        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":999999,\"memberId\":999999}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Book not found"));
        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":999999}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Member not found"));
        mvc.perform(get("/members/999999/reservations"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Member not found"));
        mvc.perform(delete("/reservations/999999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Reservation not found"));
        assertEquals(0, reservations.count());
    }

    @Test
    void ac5_inactiveMemberReturns409() throws Exception {
        unavailableBook(1L);
        Member member = members.findById(1L).orElseThrow();
        member.setActive(false);
        members.saveAndFlush(member);

        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Member is inactive."));
        assertEquals(0, reservations.count());
    }

    @Test
    void ac6_invalidAndMalformedInputsReturn400() throws Exception {
        for (String body : List.of("{}", "{\"bookId\":0,\"memberId\":-1}", "{malformed",
                "{\"bookId\":\"invalid\",\"memberId\":1}")) {
            mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, reservations.count());
    }

    @Test
    void serverOwnedFieldsCannotBeOverriddenByClientJson() throws Exception {
        unavailableBook(1L);
        String response = mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":1,\"id\":999999,"
                                + "\"status\":\"CANCELLED\",\"reservationDate\":\"1900-01-01\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode saved = mapper.readTree(response);
        assertNotEquals(999999, saved.get("id").asLong());
        assertEquals("ACTIVE", saved.get("status").asText());
        assertEquals(LocalDate.now().toString(), saved.get("reservationDate").asText());
    }

    private JsonNode reserve(long bookId, long memberId) throws Exception {
        String response = mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new ReservationController.ReservationRequest(bookId, memberId))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response);
    }

    private void unavailableBook(Long id) {
        Book book = books.findById(id).orElseThrow();
        book.setAvailable(false);
        books.saveAndFlush(book);
    }
}
