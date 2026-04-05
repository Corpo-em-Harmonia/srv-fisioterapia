package com.thalia.fisioterapia.web.dto.avaliacao;

import lombok.Data;

@Data
public class FinalizarAvaliacaoRequest {

    private String avaliacaoId;
    private String medico;
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
