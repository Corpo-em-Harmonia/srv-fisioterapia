package com.thalia.fisioterapia.web.dto.sessao;

public record AgendarSessoesResponse(
        String modo,
        String serieId,
        int quantidadeSessoes,
        String mensagem
) {}
