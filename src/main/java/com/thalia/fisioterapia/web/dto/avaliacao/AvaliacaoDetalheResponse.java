package com.thalia.fisioterapia.web.dto.avaliacao;

public record AvaliacaoDetalheResponse(
        String id,
        String pacienteId,
        String status,
        String medico,
        String hda,
        String hpp,
        String diagnostico,
        String testesRealizados,
        String goniometria,
        String condutaTerapeutica,
        String prognostico,
        String desfecho,
        String comodidade,
        String medicamentos,
        String cirurgia,
        String criadaEm,
        String finalizadaEm
) {}
