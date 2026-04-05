package com.thalia.fisioterapia.web.dto.avaliacao;

public record AvaliacaoHistoricoResponse(
        String paciente,
        String data,
        String resumo,
        String idAvaliacao
) {
}
