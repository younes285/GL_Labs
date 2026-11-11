package com.library.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GlobalExceptionHandlerTest {
    @Autowired private MockMvc mvc;

    @Test
    void missingBookReturns404WithConsistentErrorBody() throws Exception {
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":999999,\"memberId\":1}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Book not found"))
                .andExpect(jsonPath("$.timestamp").isString()).andExpect(jsonPath("$.detail").doesNotExist());
    }

    @Test
    void unavailableBookReturns409WithConsistentErrorBody() throws Exception {
        String body = "{\"bookId\":1,\"memberId\":1}";
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Book is already borrowed"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void validationReturns400AndFieldErrors() throws Exception {
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.fieldErrors.bookId").isString())
                .andExpect(jsonPath("$.fieldErrors.memberId").isString());
    }

    @Test
    void malformedJsonAndPathParametersReturn400() throws Exception {
        mvc.perform(post("/borrowings").contentType(MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.timestamp").isString());
        mvc.perform(get("/books/not-a-number"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingInternalDetails() throws Exception {
        MockMvc isolated = MockMvcBuilders.standaloneSetup(new FailureFixture())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        isolated.perform(get("/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @RestController
    static class FailureFixture {
        @GetMapping("/failure")
        public String fail() {
            throw new RuntimeException("Private database connection details must not be exposed");
        }
    }
}
