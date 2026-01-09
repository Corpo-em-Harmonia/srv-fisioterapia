package com.thalia.fisioterapia.dto;

import lombok.Data;

@Data
public class CriarLeadRequest {

    private String nome;
    private String sobrenome;
    private String email;

    private String celular;
    private String motivo;

}
