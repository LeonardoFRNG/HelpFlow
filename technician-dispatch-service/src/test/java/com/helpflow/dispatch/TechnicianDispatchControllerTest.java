package com.helpflow.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Technician Dispatch Controller Integration Tests")
class TechnicianDispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /technician-dispatch with valid body returns 200 and expected payload")
    void testDispatchEndpointSuccess() throws Exception {
        String json = """
            {
              "ticketId": 125,
              "categoria": "RED",
              "prioridad": "ALTA"
            }
            """;

        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(125))
                .andExpect(jsonPath("$.technicianId").isNotEmpty())
                .andExpect(jsonPath("$.availability").isNotEmpty())
                .andExpect(jsonPath("$.estimatedHours").isNumber());
    }

    @Test
    @DisplayName("POST /technician-dispatch with invalid body returns 400 ProblemDetail")
    void testDispatchEndpointValidationError() throws Exception {
        String invalidJson = """
            {
              "ticketId": -5
            }
            """;

        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
