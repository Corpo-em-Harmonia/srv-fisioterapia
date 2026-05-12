package com.thalia.fisioterapia.web.dto.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CriarLeadRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 80, message = "Nome deve ter entre 2 e 80 caracteres")
    private String nome;

    @NotBlank(message = "Sobrenome é obrigatório")
    @Size(min = 2, max = 80, message = "Sobrenome deve ter entre 2 e 80 caracteres")
    private String sobrenome;

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "^[\\d\\s()\\-+]{7,20}$", message = "Telefone inválido")
    private String telefone;

    @Email(message = "E-mail inválido")
    @NotBlank(message = "E-mail é obrigatório")
    @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres")
    private String email;

    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    private String observacao;
}
