package com.thalia.fisioterapia.web.dto.avaliacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FinalizarAvaliacaoRequest {

    @NotBlank(message = "ID da avaliação é obrigatório")
    private String avaliacaoId;

    @NotBlank(message = "Nome do médico/fisio é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String medico;

    @Size(max = 3000, message = "HDA deve ter no máximo 3000 caracteres")
    private String hda;

    @Size(max = 3000, message = "HPP deve ter no máximo 3000 caracteres")
    private String hpp;

    @Size(max = 1000, message = "Diagnóstico deve ter no máximo 1000 caracteres")
    private String diagnostico;

    @Size(max = 2000, message = "Testes realizados deve ter no máximo 2000 caracteres")
    private String testesRealizados;

    @Size(max = 1000, message = "Goniometria deve ter no máximo 1000 caracteres")
    private String goniometria;

    @Size(max = 3000, message = "Conduta terapêutica deve ter no máximo 3000 caracteres")
    private String condutaTerapeutica;

    @Size(max = 500, message = "Prognóstico deve ter no máximo 500 caracteres")
    private String prognostico;

    @Size(max = 500, message = "Desfecho deve ter no máximo 500 caracteres")
    private String desfecho;

    @Size(max = 500, message = "Comodidade deve ter no máximo 500 caracteres")
    private String comodidade;

    @Size(max = 1000, message = "Medicamentos deve ter no máximo 1000 caracteres")
    private String medicamentos;

    @Size(max = 500, message = "Cirurgia deve ter no máximo 500 caracteres")
    private String cirurgia;
}
