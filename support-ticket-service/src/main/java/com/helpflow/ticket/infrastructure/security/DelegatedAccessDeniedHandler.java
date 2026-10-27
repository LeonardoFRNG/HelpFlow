package com.helpflow.ticket.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpflow.ticket.infrastructure.observability.HelpFlowMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DelegatedAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private HelpFlowMetrics metrics;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        if (metrics != null) {
            metrics.recordAccessDenied();
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        String traceId = (String) request.getAttribute("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://helpflow.com/errors/forbidden");
        problem.put("title", "Forbidden");
        problem.put("status", HttpStatus.FORBIDDEN.value());
        problem.put("detail", "Access is denied: You do not have the required permissions for this resource");
        problem.put("instance", request.getRequestURI());
        problem.put("timestamp", Instant.now().toString());
        problem.put("traceId", traceId);

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
