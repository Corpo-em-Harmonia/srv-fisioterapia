package com.thalia.fisioterapia.web.dto.sessao;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RemarcarSessaoRequest(
        @NotNull(message = "Data e hora são obrigatórias") Instant dataHora,
        String escopo,
        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres") String motivo
) {}
