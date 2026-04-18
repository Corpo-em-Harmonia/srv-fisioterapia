package com.thalia.fisioterapia.web.dto.agenda;

public record AgendarAvaliacaoResponse(
        String modoAgendamento,
        String serieId,
        int sessoesGeradas,
        String sessaoInicialId,
        String mensagem
) {}
