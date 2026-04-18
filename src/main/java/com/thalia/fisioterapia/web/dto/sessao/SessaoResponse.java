package com.thalia.fisioterapia.web.dto.sessao;

public record SessaoResponse(
        String id,
        String leadId,
        String pacienteId,
        String pacienteNome,
        String pacienteTelefone,
        String dataHora,
        String status,
        String tipo,
        String serieId,
        Integer numeroOcorrencia
) {}
