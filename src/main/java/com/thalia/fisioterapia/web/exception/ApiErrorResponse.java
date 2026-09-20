package com.thalia.fisioterapia.web.exception;

import java.time.Instant;

/**
 * "mensagem" é o campo lido pelo front (mesmo nome usado em AgendaConflictResponse e
 * PlanoForaValidadeResponse). "message" é mantido por compatibilidade.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensagem,
        String message
) {
}
