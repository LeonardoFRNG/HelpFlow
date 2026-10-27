package com.helpflow.ticket.integration;

import com.helpflow.ticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Priority;
import com.helpflow.ticket.domain.model.Role;
import com.helpflow.ticket.domain.model.Ticket;
import com.helpflow.ticket.domain.model.TicketStatus;
import com.helpflow.ticket.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@DisplayName("PostgreSQL Testcontainers Persistence Integration Test")
class PostgresDatabaseTestcontainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("helpflow_test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private UserRepositoryPort userRepository;

    @Autowired
    private TechnicianRepositoryPort technicianRepository;

    @Autowired
    private TicketRepositoryPort ticketRepository;

    @Test
    @DisplayName("Flyway migrations execute cleanly and seed data is available")
    void testFlywaySeedData() {
        Optional<User> admin = userRepository.findByCorporateEmail("admin@helpflow.com");
        assertTrue(admin.isPresent(), "Admin seed user should be present from V3 migration");
        assertEquals(Role.ROLE_ADMIN, admin.get().getRole());

        assertEquals(20, technicianRepository.findAll().size(), "All 20 technicians should be seeded");
        assertTrue(technicianRepository.findByCode("TEC-014").isPresent());
    }

    @Test
    @DisplayName("Persisting ticket aggregate saves ticket, assignment and histories transactionally")
    void testTicketAggregatePersistence() {
        User user = userRepository.findByCorporateEmail("usuario@helpflow.com").orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        Ticket ticket = Ticket.createNew(
                user.getId(),
                user.getCorporateEmail(),
                "Integration test ticket",
                "Testing real postgres persistence container",
                Category.HARDWARE,
                Priority.HIGH,
                "Sede Principal",
                "PC-999",
                now.plusHours(4),
                now
        );

        ticket.assignTechnician(14L, "TEC-014", "admin@helpflow.com", "Assigned via integration test", now);
        ticket.updateStatus(TicketStatus.IN_PROGRESS, "tecnico@helpflow.com", "Started work on hardware", now);

        Ticket saved = ticketRepository.save(ticket);
        assertNotNull(saved.getId());

        // Verify retrieval with full child collections
        Optional<Ticket> fetchedOpt = ticketRepository.findById(saved.getId());
        assertTrue(fetchedOpt.isPresent());

        Ticket fetched = fetchedOpt.get();
        assertEquals(TicketStatus.IN_PROGRESS, fetched.getStatus());
        assertEquals(1, fetched.getAssignments().size());
        assertEquals("TEC-014", fetched.getAssignments().get(0).getTechnicianCode());
        assertEquals(3, fetched.getHistories().size()); // 1 initial + 1 assignment + 1 progress
    }
}
