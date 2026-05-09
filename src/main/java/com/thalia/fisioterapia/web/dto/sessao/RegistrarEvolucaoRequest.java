package com.thalia.fisioterapia.web.dto.sessao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record RegistrarEvolucaoRequest(
        String observacoes,
        @Min(0) @Max(10) Integer nivelDor,
        @Min(0) @Max(100) Integer mobilidade,
        List<String> exercicios
) {}
