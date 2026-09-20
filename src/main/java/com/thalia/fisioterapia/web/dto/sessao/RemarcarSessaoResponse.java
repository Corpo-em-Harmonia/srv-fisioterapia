package com.thalia.fisioterapia.web.dto.sessao;

public record RemarcarSessaoResponse(
        int sessoesAfetadas,
        String serieId,
        String escopoAplicado
) {}
