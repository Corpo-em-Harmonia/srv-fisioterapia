package com.thalia.fisioterapia.domain.sessao;

public enum ModoAgendamento {
    AVULSO,
    RECORRENTE;

    public static ModoAgendamento fromNullable(String valor) {
        if (valor == null || valor.isBlank()) {
            return AVULSO;
        }
        return ModoAgendamento.valueOf(valor.trim().toUpperCase());
    }
}
