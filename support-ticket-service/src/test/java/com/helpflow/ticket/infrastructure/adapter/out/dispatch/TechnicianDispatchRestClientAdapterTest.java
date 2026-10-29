package com.helpflow.ticket.infrastructure.adapter.out.dispatch;

import com.helpflow.ticket.application.port.out.DispatchResult;
import com.helpflow.ticket.domain.model.Availability;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("Technician Dispatch REST Client Adapter Tests")
class TechnicianDispatchRestClientAdapterTest {

    @Test
    @DisplayName("Successful dispatch call maps response correctly")
    void testDispatchSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        TechnicianDispatchRestClientAdapter adapter =
                new TechnicianDispatchRestClientAdapter(builder, "http://localhost:8081");

        String responseJson = """
            {
              "ticketId": 125,
              "technicianId": "TEC-014",
              "availability": "MEDIUM",
              "estimatedHours": 4
            }
            """;

        server.expect(requestTo("http://localhost:8081/technician-dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        DispatchResult result = adapter.dispatch(125L, Category.NETWORK, Priority.HIGH);

        assertNotNull(result);
        assertEquals(125L, result.ticketId());
        assertEquals("TEC-014", result.technicianId());
        assertEquals(Availability.MEDIUM, result.availability());
        assertEquals(4, result.estimatedHours());

        server.verify();
    }

    @Test
    @DisplayName("Downstream error throws RuntimeException with clear error context")
    void testDispatchError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        TechnicianDispatchRestClientAdapter adapter =
                new TechnicianDispatchRestClientAdapter(builder, "http://localhost:8081");

        server.expect(requestTo("http://localhost:8081/technician-dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(RuntimeException.class, () ->
                adapter.dispatch(999L, Category.HARDWARE, Priority.CRITICAL)
        );

        server.verify();
    }
}
