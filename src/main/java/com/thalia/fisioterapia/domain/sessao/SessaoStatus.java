package com.thalia.fisioterapia.domain.sessao;

public enum SessaoStatus {
    MARCADA,
    REMARCADA,
    COMPARECEU,
    AGUARDANDO_AVALIACAO,
    AVALIADA,
    REALIZADA,            // Sessão concluída com evolução registrada pelo fisio
    FALTOU,
    CANCELADA
}