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
