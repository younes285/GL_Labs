package com.library.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void everyBusinessEndpointHasSummaryTagStatusesAndDtoSchemas() throws Exception {
        JsonNode docs = mapper.readTree(mvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        int operations = 0;
        for (JsonNode path : docs.get("paths")) {
            for (JsonNode operation : path) {
                assertFalse(operation.path("summary").asText().isBlank());
                assertFalse(operation.path("tags").isEmpty());
                assertTrue(operation.path("responses").has("500"), operation.toString());
                operations++;
            }
        }
        assertEquals(14, operations);
        JsonNode schemas = docs.path("components").path("schemas");
        for (String name : List.of("BookDTO", "MemberDTO", "BorrowingDTO", "ReservationDTO", "ErrorResponse")) {
            assertTrue(schemas.has(name), "Missing schema: " + name);
            for (JsonNode property : schemas.get(name).get("properties")) {
                assertTrue(property.has("example"), "Missing example in " + name + ": " + property);
            }
        }
        for (String name : List.of("Book", "Member", "Borrowing", "Reservation")) {
            assertFalse(schemas.has(name), "JPA entity leaked into OpenAPI: " + name);
        }
        assertTrue(docs.at("/paths/~1borrowings/post/responses").has("404"));
        assertTrue(docs.at("/paths/~1borrowings/post/responses").has("409"));
        assertEquals("#/components/schemas/BorrowingDTO",
                docs.at("/paths/~1borrowings/post/responses/201/content/application~1json/schema/$ref").asText());
        assertEquals("#/components/schemas/BookDTO",
                docs.at("/paths/~1books/get/responses/200/content/application~1json/schema/items/$ref").asText());
    }
}
