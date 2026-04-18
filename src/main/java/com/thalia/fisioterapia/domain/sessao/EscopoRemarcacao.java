package com.thalia.fisioterapia.domain.sessao;

public enum EscopoRemarcacao {
    SOMENTE_ESTA,
    ESTA_E_PROXIMAS,
    TODA_SERIE;

    public static EscopoRemarcacao fromNullable(String valor) {
        if (valor == null || valor.isBlank()) {
            return SOMENTE_ESTA;
        }
        return EscopoRemarcacao.valueOf(valor.trim().toUpperCase());
    }
}
