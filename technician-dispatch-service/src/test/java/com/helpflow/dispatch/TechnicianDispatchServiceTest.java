package com.helpflow.dispatch;

import com.helpflow.dispatch.dto.DispatchRequest;
import com.helpflow.dispatch.dto.DispatchResponse;
import com.helpflow.dispatch.service.TechnicianDispatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Technician Dispatch Service Unit Tests")
class TechnicianDispatchServiceTest {

    private final TechnicianDispatchService service = new TechnicianDispatchService();

    @Test
    @DisplayName("Same input combination always produces the exact same technician and availability (Determinism)")
    void testDeterminism() {
        DispatchRequest req1 = new DispatchRequest(125L, "RED", "ALTA");
        DispatchRequest req2 = new DispatchRequest(125L, "RED", "ALTA");

        DispatchResponse res1 = service.dispatch(req1);
        DispatchResponse res2 = service.dispatch(req2);

        assertEquals(res1.technicianId(), res2.technicianId());
        assertEquals(res1.availability(), res2.availability());
        assertEquals(res1.estimatedHours(), res2.estimatedHours());
        assertEquals(125L, res1.ticketId());
    }

    @Test
    @DisplayName("Different inputs can yield different technicians or times")
    void testDifferentInputs() {
        DispatchRequest req1 = new DispatchRequest(100L, "HARDWARE", "LOW");
        DispatchRequest req2 = new DispatchRequest(999L, "ACCESS", "CRITICAL");

        DispatchResponse res1 = service.dispatch(req1);
        DispatchResponse res2 = service.dispatch(req2);

        // One of the fields should differ
        assertTrue(!res1.technicianId().equals(res2.technicianId())
                || res1.estimatedHours() != res2.estimatedHours()
                || !res1.availability().equals(res2.availability()));
    }

    @Test
    @DisplayName("Availability classification corresponds to hours rules")
    void testAvailabilityClassification() {
        for (long ticketId = 1; ticketId <= 50; ticketId++) {
            DispatchRequest req = new DispatchRequest(ticketId, "SOFTWARE", "MEDIUM");
            DispatchResponse res = service.dispatch(req);

            if (res.estimatedHours() <= 2) {
                assertEquals("HIGH", res.availability());
                assertEquals("ALTA", res.disponibilidad());
            } else if (res.estimatedHours() <= 8) {
                assertEquals("MEDIUM", res.availability());
                assertEquals("MEDIA", res.disponibilidad());
            } else {
                assertEquals("LOW", res.availability());
                assertEquals("BAJA", res.disponibilidad());
            }
        }
    }
}
