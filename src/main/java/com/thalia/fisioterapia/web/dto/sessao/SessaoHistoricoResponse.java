package com.thalia.fisioterapia.web.dto.sessao;

import java.util.List;

public record SessaoHistoricoResponse(
        String id,
        Integer numeroOcorrencia,
        String dataHora,
        String status,
        String tipo,
        String observacoes,
        Integer nivelDor,
        Integer mobilidade,
        List<String> exercicios,
        String avaliacaoId
) {}
