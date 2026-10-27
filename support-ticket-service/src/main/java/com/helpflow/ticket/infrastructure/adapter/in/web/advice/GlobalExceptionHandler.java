package com.helpflow.ticket.infrastructure.adapter.in.web.advice;

import com.helpflow.ticket.domain.exception.DomainException;
import com.helpflow.ticket.domain.exception.InvalidStateTransitionException;
import com.helpflow.ticket.domain.exception.TechnicianNotFoundException;
import com.helpflow.ticket.domain.exception.TicketNotFoundException;
import com.helpflow.ticket.domain.exception.UnauthorizedTicketAccessException;
import com.helpflow.ticket.domain.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return buildProblem(HttpStatus.BAD_REQUEST, "Validation Error",
                "https://helpflow.com/errors/validation", detail, request);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransition(InvalidStateTransitionException ex, HttpServletRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return buildProblem(HttpStatus.CONFLICT, "Invalid State Transition",
                "https://helpflow.com/errors/invalid-transition", ex.getMessage(), request);
    }

    @ExceptionHandler({TicketNotFoundException.class, UserNotFoundException.class, TechnicianNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(DomainException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, "Resource Not Found",
                "https://helpflow.com/errors/not-found", ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedTicketAccessException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorizedAccess(UnauthorizedTicketAccessException ex, HttpServletRequest request) {
        log.warn("Access denied to ticket: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Forbidden Ticket Access",
                "https://helpflow.com/errors/forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblem(HttpStatus.FORBIDDEN, "Forbidden",
                "https://helpflow.com/errors/forbidden",
                "You do not have required permissions for this action", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildProblem(HttpStatus.UNAUTHORIZED, "Authentication Failed",
                "https://helpflow.com/errors/unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Business Rule Violation",
                "https://helpflow.com/errors/business-rule", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid Argument",
                "https://helpflow.com/errors/invalid-argument", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "https://helpflow.com/errors/internal",
                "An unexpected server error occurred. Please contact IT support.", request);
    }

    private ResponseEntity<ProblemDetail> buildProblem(HttpStatus status, String title, String typeUri,
                                                       String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(typeUri));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());

        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = (String) request.getAttribute("traceId");
        }
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        problem.setProperty("traceId", traceId);

        return ResponseEntity.status(status).body(problem);
    }
}
