package com.helpflow.ticket.infrastructure.adapter.out.dispatch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.helpflow.ticket.application.port.out.DispatchResult;
import com.helpflow.ticket.application.port.out.TechnicianDispatchPort;
import com.helpflow.ticket.domain.model.Availability;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TechnicianDispatchRestClientAdapter implements TechnicianDispatchPort {

    private static final Logger log = LoggerFactory.getLogger(TechnicianDispatchRestClientAdapter.class);

    private final RestClient restClient;
    private final String dispatchBaseUrl;

    public TechnicianDispatchRestClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${dispatch.service.url:http://localhost:8081}") String dispatchBaseUrl) {
        this.dispatchBaseUrl = dispatchBaseUrl;
        this.restClient = restClientBuilder
                .baseUrl(dispatchBaseUrl)
                .build();
    }

    record DispatchClientRequest(Long ticketId, String category, String priority) {}

    record DispatchClientResponse(
            @JsonProperty("ticketId") Long ticketId,
            @JsonProperty("technicianId") String technicianId,
            @JsonProperty("availability") String availability,
            @JsonProperty("estimatedHours") int estimatedHours
    ) {}

    @Override
    public DispatchResult dispatch(Long ticketId, Category category, Priority priority) {
        log.info("Calling technician-dispatch-service at {}/technician-dispatch for ticketId={}, category={}, priority={}",
                dispatchBaseUrl, ticketId, category, priority);

        try {
            DispatchClientRequest request = new DispatchClientRequest(
                    ticketId,
                    category.name(),
                    priority.name()
            );

            DispatchClientResponse response = restClient.post()
                    .uri("/technician-dispatch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DispatchClientResponse.class);

            if (response != null) {
                Availability availability = Availability.fromString(response.availability());
                return new DispatchResult(
                        response.ticketId(),
                        response.technicianId(),
                        availability,
                        response.estimatedHours()
                );
            }
        } catch (Exception ex) {
            log.error("Failed to communicate with technician-dispatch-service at {}: {}", dispatchBaseUrl, ex.getMessage());
            throw new RuntimeException("External technician dispatch service unavailable: " + ex.getMessage(), ex);
        }

        return null;
    }
}
