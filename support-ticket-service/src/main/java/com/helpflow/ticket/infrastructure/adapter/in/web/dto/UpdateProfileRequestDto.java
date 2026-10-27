package com.helpflow.ticket.infrastructure.adapter.in.web.dto;

public record UpdateProfileRequestDto(
        String name,
        String location,
        String area
) {}
