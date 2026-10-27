package com.helpflow.ticket.application;

import com.helpflow.ticket.application.port.in.TechnicianManagementUseCase;
import com.helpflow.ticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.ticket.application.service.TechnicianApplicationService;
import com.helpflow.ticket.domain.exception.BusinessRuleException;
import com.helpflow.ticket.domain.exception.TechnicianNotFoundException;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.TechnicianStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Technician Application Service Unit Tests")
class TechnicianApplicationServiceTest {

    @Mock
    private TechnicianRepositoryPort technicianRepository;

    @InjectMocks
    private TechnicianApplicationService technicianService;

    private Technician sampleTech;

    @BeforeEach
    void setUp() {
        sampleTech = new Technician(1L, "TEC-010", "DOC-T10", "Tech Ten", "tech10@helpflow.com",
                "HQ", Set.of(Category.SOFTWARE), TechnicianStatus.ACTIVE, 5);
    }

    @Test
    @DisplayName("Create technician successfully saves and returns technician")
    void testCreateTechnicianSuccess() {
        when(technicianRepository.findByCode("TEC-010")).thenReturn(Optional.empty());
        when(technicianRepository.findByDocument("DOC-T10")).thenReturn(Optional.empty());
        when(technicianRepository.save(any(Technician.class))).thenReturn(sampleTech);

        TechnicianManagementUseCase.CreateTechnicianCommand cmd = new TechnicianManagementUseCase.CreateTechnicianCommand(
                "TEC-010", "DOC-T10", "Tech Ten", "tech10@helpflow.com", "HQ", Set.of(Category.SOFTWARE), 5
        );

        Technician created = technicianService.createTechnician(cmd);
        assertNotNull(created);
        assertEquals("TEC-010", created.getCode());
    }

    @Test
    @DisplayName("Create technician with duplicate code throws BusinessRuleException")
    void testDuplicateCodeThrows() {
        when(technicianRepository.findByCode("TEC-010")).thenReturn(Optional.of(sampleTech));

        TechnicianManagementUseCase.CreateTechnicianCommand cmd = new TechnicianManagementUseCase.CreateTechnicianCommand(
                "TEC-010", "DOC-T99", "Tech Ten", "tech10@helpflow.com", "HQ", Set.of(Category.SOFTWARE), 5
        );

        assertThrows(BusinessRuleException.class, () -> technicianService.createTechnician(cmd));
        verify(technicianRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create technician with duplicate document throws BusinessRuleException")
    void testDuplicateDocumentThrows() {
        when(technicianRepository.findByCode("TEC-099")).thenReturn(Optional.empty());
        when(technicianRepository.findByDocument("DOC-T10")).thenReturn(Optional.of(sampleTech));

        TechnicianManagementUseCase.CreateTechnicianCommand cmd = new TechnicianManagementUseCase.CreateTechnicianCommand(
                "TEC-099", "DOC-T10", "Tech Ten", "tech10@helpflow.com", "HQ", Set.of(Category.SOFTWARE), 5
        );

        assertThrows(BusinessRuleException.class, () -> technicianService.createTechnician(cmd));
        verify(technicianRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get technician by ID and by code")
    void testGetTechnician() {
        when(technicianRepository.findById(1L)).thenReturn(Optional.of(sampleTech));
        when(technicianRepository.findByCode("TEC-010")).thenReturn(Optional.of(sampleTech));
        when(technicianRepository.findAll()).thenReturn(List.of(sampleTech));

        assertEquals("TEC-010", technicianService.getTechnicianById(1L).getCode());
        assertEquals("TEC-010", technicianService.getTechnicianByCode("TEC-010").getCode());
        assertEquals(1, technicianService.getAllTechnicians().size());
    }

    @Test
    @DisplayName("Get non-existent technician throws TechnicianNotFoundException")
    void testNotFoundThrows() {
        when(technicianRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(TechnicianNotFoundException.class, () -> technicianService.getTechnicianById(99L));
    }

    @Test
    @DisplayName("Toggle technician status updates state")
    void testToggleStatus() {
        when(technicianRepository.findById(1L)).thenReturn(Optional.of(sampleTech));
        when(technicianRepository.save(any(Technician.class))).thenAnswer(i -> i.getArgument(0));

        Technician deactivated = technicianService.toggleTechnicianStatus(1L, false);
        assertEquals(TechnicianStatus.INACTIVE, deactivated.getStatus());

        Technician activated = technicianService.toggleTechnicianStatus(1L, true);
        assertEquals(TechnicianStatus.ACTIVE, activated.getStatus());
    }
}
