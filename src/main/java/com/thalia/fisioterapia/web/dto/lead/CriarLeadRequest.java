package com.thalia.fisioterapia.web.dto.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CriarLeadRequest {

    @NotBlank
    private String nome;
    @NotBlank
    private String sobrenome;
    @NotBlank private String telefone;
    @Email
    @NotBlank private String email;

    private LocalDateTime criadoEm; // opcional, se não for enviado, o backend pode definir como LocalDateTime.now()
    private String observacao; // opcional

}
