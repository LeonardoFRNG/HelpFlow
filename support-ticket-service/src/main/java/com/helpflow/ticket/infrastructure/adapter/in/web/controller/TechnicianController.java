package com.helpflow.ticket.infrastructure.adapter.in.web.controller;

import com.helpflow.ticket.application.port.in.TechnicianManagementUseCase;
import com.helpflow.ticket.domain.model.Technician;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.CreateTechnicianRequestDto;
import com.helpflow.ticket.infrastructure.adapter.in.web.dto.TechnicianResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/technicians")
@Tag(name = "Technicians", description = "Technician roster, specialties and availability management")
@SecurityRequirement(name = "bearerAuth")
public class TechnicianController {

    private final TechnicianManagementUseCase technicianManagementUseCase;

    public TechnicianController(TechnicianManagementUseCase technicianManagementUseCase) {
        this.technicianManagementUseCase = technicianManagementUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Register technician", description = "Admin creation of technician profile with assigned specialties")
    public ResponseEntity<TechnicianResponseDto> createTechnician(@Valid @RequestBody CreateTechnicianRequestDto request) {
        TechnicianManagementUseCase.CreateTechnicianCommand command = new TechnicianManagementUseCase.CreateTechnicianCommand(
                request.code(),
                request.document(),
                request.name(),
                request.email(),
                request.location(),
                request.specialties(),
                request.maxActiveTickets()
        );
        Technician created = technicianManagementUseCase.createTechnician(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TechnicianResponseDto.fromDomain(created));
    }

    @GetMapping
    @Operation(summary = "List all technicians", description = "Retrieves active and inactive technicians and their specialties")
    public ResponseEntity<List<TechnicianResponseDto>> getAllTechnicians() {
        List<Technician> technicians = technicianManagementUseCase.getAllTechnicians();
        return ResponseEntity.ok(technicians.stream().map(TechnicianResponseDto::fromDomain).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get technician by ID", description = "Retrieves specific technician profile and specialties")
    public ResponseEntity<TechnicianResponseDto> getTechnicianById(@PathVariable Long id) {
        Technician tech = technicianManagementUseCase.getTechnicianById(id);
        return ResponseEntity.ok(TechnicianResponseDto.fromDomain(tech));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Toggle technician active status", description = "Admin-only activation or deactivation of technician")
    public ResponseEntity<TechnicianResponseDto> toggleStatus(@PathVariable Long id, @RequestParam boolean active) {
        Technician updated = technicianManagementUseCase.toggleTechnicianStatus(id, active);
        return ResponseEntity.ok(TechnicianResponseDto.fromDomain(updated));
    }
}
