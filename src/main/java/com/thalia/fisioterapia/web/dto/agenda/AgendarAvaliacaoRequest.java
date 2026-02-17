package com.thalia.fisioterapia.web.dto.agenda;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendarAvaliacaoRequest(
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataHora,
        String observacao // opcional (depois você pode salvar no Lead ou numa "anotação")
) {}
