package com.helpflow.ticket.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpflow.ticket.infrastructure.observability.HelpFlowMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Maneja accesos no autenticados (401) generando un ProblemDetail con traceId
@Component
public class DelegatedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private HelpFlowMetrics metrics;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        if (metrics != null) {
            metrics.recordAuthFailure();
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        String traceId = (String) request.getAttribute("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://helpflow.com/errors/unauthorized");
        problem.put("title", "Unauthorized");
        problem.put("status", HttpStatus.UNAUTHORIZED.value());
        problem.put("detail", "Authentication is required to access this resource: " + authException.getMessage());
        problem.put("instance", request.getRequestURI());
        problem.put("timestamp", Instant.now().toString());
        problem.put("traceId", traceId);

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
