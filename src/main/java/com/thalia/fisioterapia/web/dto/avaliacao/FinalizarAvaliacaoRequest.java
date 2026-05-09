package com.thalia.fisioterapia.web.dto.avaliacao;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FinalizarAvaliacaoRequest {

    @NotBlank
    private String avaliacaoId;

    @NotBlank
    private String medico;

    private String leadId;
    private String hda;
    private String hpp;
    private String diagnostico;
    private String testesRealizados;
    private String goniometria;
    private String condutaTerapeutica;
    private String prognostico;
    private String desfecho;
    private String comodidade;
    private String medicamentos;
    private String cirurgia;
}
