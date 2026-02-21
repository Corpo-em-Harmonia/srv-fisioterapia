package com.thalia.fisioterapia.domain.paciente;

import com.thalia.fisioterapia.domain.lead.Lead;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;


@Document(collection = "pacientes")
@Getter
public class Paciente {

    @Id
    private String id;

    private String leadId;

    private String nome;
    private String sobrenome;
    private String telefone;
    private String email;

    private String dataNascimento;
    private String cpf;
    private Boolean cirurgico;
    private String profissao;
    private LocalDateTime criadoEm;
    private int totalFaltas = 0;
    private int totalComparecimentos = 0;

    private Paciente() {
        // construtor para o Mongo
    }

    private Paciente(String nome, String telefone, String email) {
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.criadoEm = LocalDateTime.now();
    }


    public static Paciente fromLead(Lead lead) {

        Paciente paciente = new Paciente();

        paciente.nome = lead.getNome();
        paciente.sobrenome = lead.getSobrenome();
        paciente.telefone = lead.getTelefone();
        paciente.email = lead.getEmail();

        return paciente;
    }

    // ✅ NOVOS MÉTODOS
    public void incrementarFaltas() {
        this.totalFaltas++;
    }

    public void incrementarComparecimentos() {
        this.totalComparecimentos++;
    }
}
