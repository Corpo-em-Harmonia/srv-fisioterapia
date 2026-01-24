package com.thalia.fisioterapia.model;

import lombok.Data;

@Data
public class Paciente {

    String nome;
    String sobrenome;
    String telefone;
    String email;
    String dataNascimento;
    String cpf;
    Boolean cirurgico;
    String profissao;

    public Paciente(String nome, String telefone) {
        this.nome = nome;
        this.telefone = telefone;
    }
}
