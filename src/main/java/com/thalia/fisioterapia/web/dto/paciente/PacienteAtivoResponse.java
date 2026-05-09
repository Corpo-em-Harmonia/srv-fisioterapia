package com.thalia.fisioterapia.web.dto.paciente;

public record PacienteAtivoResponse(
        String idPaciente,
        String nome,
        String ultimaSessao,
        String proximaSessao,
        long totalSessoes,
        long sessoesRealizadas,
        String statusClinico
) {
}
