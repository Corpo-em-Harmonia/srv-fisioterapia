package com.thalia.fisioterapia.web.dto.avaliacao;

public record AvaliacaoPendenteResponse(
        String idSessao,
        String idLead,
        String idPaciente,
        String nome,
        String telefone,
        String dataHora,
        String status,
        String origem
) {
}
