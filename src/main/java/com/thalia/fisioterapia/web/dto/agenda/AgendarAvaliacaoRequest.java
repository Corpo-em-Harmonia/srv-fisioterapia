package com.thalia.fisioterapia.web.dto.agenda;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AgendarAvaliacaoRequest(
        @NotNull Instant dataHora,
        String observacao // opcional (depois você pode salvar no Lead ou numa “anotação”)
) {}
