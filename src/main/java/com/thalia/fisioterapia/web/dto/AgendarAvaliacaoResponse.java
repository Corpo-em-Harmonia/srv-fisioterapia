package com.thalia.fisioterapia.web.dto;

import java.time.Instant;

public record AgendarAvaliacaoResponse(
        String leadId,
        String sessaoId,
        Instant dataHora,
        String leadStatus,
        String sessaoStatus
) {}
