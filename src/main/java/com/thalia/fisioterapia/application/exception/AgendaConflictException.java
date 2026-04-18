package com.thalia.fisioterapia.application.exception;

import java.time.Instant;
import java.util.List;

public class AgendaConflictException extends RuntimeException {

    private final List<ConflitoAgendaItem> conflitos;

    public AgendaConflictException(String message, List<ConflitoAgendaItem> conflitos) {
        super(message);
        this.conflitos = conflitos;
    }

    public List<ConflitoAgendaItem> getConflitos() {
        return conflitos;
    }

    public record ConflitoAgendaItem(
            String sessaoId,
            Instant dataHora,
            String pacienteNome
    ) {
    }
}
