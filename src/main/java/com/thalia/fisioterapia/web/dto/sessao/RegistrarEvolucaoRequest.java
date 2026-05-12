package com.thalia.fisioterapia.web.dto.sessao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegistrarEvolucaoRequest(
        @NotBlank(message = "Observações da evolução são obrigatórias")
        @Size(max = 2000, message = "Observações devem ter no máximo 2000 caracteres")
        String observacoes,
        @Min(value = 0, message = "Nível de dor deve ser entre 0 e 10")
        @Max(value = 10, message = "Nível de dor deve ser entre 0 e 10")
        Integer nivelDor,
        @Min(value = 0, message = "Mobilidade deve ser entre 0 e 100")
        @Max(value = 100, message = "Mobilidade deve ser entre 0 e 100")
        Integer mobilidade,
        @Size(max = 20, message = "Máximo de 20 exercícios por sessão")
        List<String> exercicios
) {}
