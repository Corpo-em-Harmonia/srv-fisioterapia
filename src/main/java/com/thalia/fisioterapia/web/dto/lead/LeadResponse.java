package com.thalia.fisioterapia.web.dto.lead;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record LeadResponse(String id,
                           String nome,
                           String sobrenome,
                           String telefone,
                           String email,
                           String observacao,
                           String status,
                           @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
                           LocalDateTime criadoEm) {
}
