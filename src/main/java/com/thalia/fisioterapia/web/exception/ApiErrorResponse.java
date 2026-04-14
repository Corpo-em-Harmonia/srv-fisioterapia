package com.thalia.fisioterapia.web.exception;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}
