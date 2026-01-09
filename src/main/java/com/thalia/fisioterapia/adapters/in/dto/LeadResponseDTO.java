package com.thalia.fisioterapia.adapters.in.dto;

import java.util.UUID;


public record LeadResponseDTO(
        UUID id,
        String nome,
        String email
) {

}
