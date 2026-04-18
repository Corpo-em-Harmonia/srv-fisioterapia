package com.thalia.fisioterapia.web.exception;

import java.util.List;

public record AgendaConflictResponse(
        String codigo,
        String mensagem,
        List<ConflitoItem> conflitos
) {
    public record ConflitoItem(
            String sessaoId,
            String dataHora,
            String pacienteNome
    ) {
    }
}
