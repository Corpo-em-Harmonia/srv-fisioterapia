package com.thalia.fisioterapia.web.dto.avaliacao;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IniciarAvaliacaoRequest {

    @NotBlank(message = "ID da avaliação é obrigatório")
    private String leadId; // representa o avaliacaoId — nome mantido por compatibilidade com frontend
}
