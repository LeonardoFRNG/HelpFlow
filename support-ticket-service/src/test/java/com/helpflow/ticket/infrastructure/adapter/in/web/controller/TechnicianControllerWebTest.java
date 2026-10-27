package com.helpflow.ticket.infrastructure.adapter.in.web.controller;

import com.helpflow.ticket.application.port.in.TechnicianManagementUseCase;
import com.helpflow.ticket.domain.model.Category;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.domain.model.TechnicianStatus;
import com.helpflow.ticket.infrastructure.adapter.in.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Technician Controller Unit Tests")
class TechnicianControllerWebTest {

    private MockMvc mockMvc;

    @Mock
    private TechnicianManagementUseCase technicianManagementUseCase;

    @InjectMocks
    private TechnicianController technicianController;

    private Technician sampleTech;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(technicianController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleTech = new Technician(1L, "TEC-050", "DOC-T50", "Tech Fifty", "tech50@helpflow.com",
                "HQ", Set.of(Category.NETWORK), TechnicianStatus.ACTIVE, 5);
    }

    @Test
    @DisplayName("GET /technicians returns list of technicians")
    void testGetAllTechnicians() throws Exception {
        when(technicianManagementUseCase.getAllTechnicians()).thenReturn(List.of(sampleTech));

        mockMvc.perform(get("/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("TEC-050"));
    }

    @Test
    @DisplayName("GET /technicians/{id} returns technician details")
    void testGetTechnicianById() throws Exception {
        when(technicianManagementUseCase.getTechnicianById(1L)).thenReturn(sampleTech);

        mockMvc.perform(get("/technicians/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEC-050"));
    }

    @Test
    @DisplayName("POST /technicians creates technician successfully")
    void testCreateTechnician() throws Exception {
        when(technicianManagementUseCase.createTechnician(any())).thenReturn(sampleTech);

        String json = """
            {
              "code": "TEC-050",
              "document": "DOC-T50",
              "name": "Tech Fifty",
              "email": "tech50@helpflow.com",
              "location": "HQ",
              "specialties": ["NETWORK"],
              "maxActiveTickets": 5
            }
            """;

        mockMvc.perform(post("/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TEC-050"));
    }

    @Test
    @DisplayName("PATCH /technicians/{id}/status toggles technician status")
    void testToggleTechnicianStatus() throws Exception {
        when(technicianManagementUseCase.toggleTechnicianStatus(1L, false)).thenReturn(sampleTech);

        mockMvc.perform(patch("/technicians/1/status?active=false"))
                .andExpect(status().isOk());
    }
}
