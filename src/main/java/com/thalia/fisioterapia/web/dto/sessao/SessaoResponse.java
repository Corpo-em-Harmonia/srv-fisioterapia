package com.thalia.fisioterapia.web.dto.sessao;

public record SessaoResponse(
        String id,
        String pacienteId,
        String pacienteNome,
        String pacienteTelefone,
        String dataHora,
        String status,
        String tipo
) {}
