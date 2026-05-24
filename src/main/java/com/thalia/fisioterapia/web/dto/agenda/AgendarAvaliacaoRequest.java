package com.thalia.fisioterapia.web.dto.agenda;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record AgendarAvaliacaoRequest(
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataHora,
        String observacao,
        String modoAgendamento,
        @Min(1) @Max(7) Integer frequenciaSemanal,
        @Positive Integer quantidadeSessoes,
        @Positive Integer validadeGuiaDias
) {}
