package com.thalia.fisioterapia.web.dto.avaliacao;

public record IniciarAvaliacaoResponse(
        String pacienteId,
        String avaliacaoId,
        String pacienteNome
) {}
