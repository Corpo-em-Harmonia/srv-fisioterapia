package com.thalia.fisioterapia.web.exception;

public record PlanoForaValidadeResponse(
        String codigo,
        String mensagem,
        Detalhes detalhes
) {
    public record Detalhes(
            int duracaoDias,
            int validadeGuiaDias,
            int frequenciaMinimaSugerida
    ) {
    }
}
