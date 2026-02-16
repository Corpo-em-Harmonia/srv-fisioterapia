package com.thalia.fisioterapia.web.dto.avaliacao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class FinalizarAvaliacaoRequest {

    @JsonProperty("lead_Id")
    private String leadId;

    private String medico;
    private String avaliacaoId;
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
