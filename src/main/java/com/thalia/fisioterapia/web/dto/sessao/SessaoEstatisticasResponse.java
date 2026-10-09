package com.thalia.fisioterapia.web.dto.sessao;

public record SessaoEstatisticasResponse(
        long hoje,
        long pendentes,
        long compareceu,
        long faltou,
        long total,
        long pessoasComFaltas,
        long pessoasQueCompareceram
) {}
