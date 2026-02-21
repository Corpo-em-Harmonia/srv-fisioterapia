package com.thalia.fisioterapia.domain.sessao;

public enum SessaoStatus {
    MARCADA,
    REMARCADA,
    COMPARECEU,
    AGUARDANDO_AVALIACAO, // ✅ Compareceu, esperando fisio avaliar
    AVALIADA,             // ✅ Fisio concluiu avaliação
    FALTOU,
    CANCELADA
}