package com.helpflow.dispatch.controller;

import com.helpflow.dispatch.dto.DispatchRequest;
import com.helpflow.dispatch.dto.DispatchResponse;
import com.helpflow.dispatch.service.TechnicianDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/technician-dispatch")
@Tag(name = "Technician Dispatch", description = "Simulated external technician availability service")
public class TechnicianDispatchController {

    private final TechnicianDispatchService dispatchService;

    public TechnicianDispatchController(TechnicianDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping
    @Operation(summary = "Dispatch technician", description = "Deterministically selects a technician and estimated time based on ticket data")
    public ResponseEntity<DispatchResponse> dispatch(@Valid @RequestBody DispatchRequest request) {
        DispatchResponse response = dispatchService.dispatch(request);
        return ResponseEntity.ok(response);
    }
}
