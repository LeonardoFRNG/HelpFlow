package com.helpflow.ticket.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Delegated Security Handlers Unit Tests")
class DelegatedSecurityHandlersTest {

    @Test
    @DisplayName("DelegatedAccessDeniedHandler writes 403 ProblemDetail JSON")
    void testAccessDeniedHandler() throws Exception {
        DelegatedAccessDeniedHandler handler = new DelegatedAccessDeniedHandler();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/test");
        request.setAttribute("traceId", "test-trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Forbidden action"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("test-trace-123"));
        assertTrue(response.getContentAsString().contains("Forbidden"));
    }

    @Test
    @DisplayName("DelegatedAuthenticationEntryPoint writes 401 ProblemDetail JSON")
    void testAuthenticationEntryPoint() throws Exception {
        DelegatedAuthenticationEntryPoint entryPoint = new DelegatedAuthenticationEntryPoint();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tickets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new org.springframework.security.authentication.BadCredentialsException("Bad token"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
    }
}
