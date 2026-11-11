package com.library.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DtoContractTest {
    @Autowired private MockMvc mvc;

    @Test
    void borrowingAndReservationResponsesDoNotExposeNestedMemberData() throws Exception {
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.book").doesNotExist()).andExpect(jsonPath("$.member").doesNotExist());
        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.memberId").value(2))
                .andExpect(jsonPath("$.member").doesNotExist()).andExpect(jsonPath("$.book").doesNotExist());
        mvc.perform(get("/members/1/borrowings"))
                .andExpect(jsonPath("$[0].member").doesNotExist());
        mvc.perform(get("/members/2/reservations"))
                .andExpect(jsonPath("$[0].member").doesNotExist());
    }

    @Test
    void optionalDtoFieldsKeepUsefulCreationDefaults() throws Exception {
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"test-isbn\",\"title\":\"Test\",\"author\":\"Author\",\"genre\":\"Reference\",\"publishedYear\":2026}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.available").value(true));
        mvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DTO Member\",\"email\":\"dto@usj.edu.lb\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.membershipDate").isString());
    }
}
