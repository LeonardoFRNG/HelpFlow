package com.helpflow.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DispatchResponse(
        @JsonProperty("ticketId")
        Long ticketId,

        @JsonProperty("technicianId")
        String technicianId,

        @JsonProperty("tecnicoId")
        String tecnicoId,

        @JsonProperty("availability")
        String availability,

        @JsonProperty("disponibilidad")
        String disponibilidad,

        @JsonProperty("estimatedHours")
        int estimatedHours,

        @JsonProperty("tiempoEstimadoHoras")
        int tiempoEstimadoHoras
) {
    public static DispatchResponse of(Long ticketId, String technicianCode, String availability,
                                      String disponibilidad, int estimatedHours) {
        return new DispatchResponse(
                ticketId,
                technicianCode,
                technicianCode,
                availability,
                disponibilidad,
                estimatedHours,
                estimatedHours
        );
    }
}
