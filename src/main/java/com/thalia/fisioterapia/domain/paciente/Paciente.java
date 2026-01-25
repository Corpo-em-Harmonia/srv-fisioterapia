package com.thalia.fisioterapia.domain.paciente;

import com.thalia.fisioterapia.domain.lead.Lead;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "pacientes")
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
    protected Paciente() {
        // construtor para o Mongo
    }

    private Paciente(String nome, String telefone, String email) {
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.criadoEm = LocalDateTime.now();
    }


    public static Paciente fromLead(Lead lead) {
        Paciente p = new Paciente();
        p.nome = lead.getNome();
        p.telefone = lead.getCelular();
        p.email = lead.getEmail();
        p.id = lead.getId();
        p.criadoEm = LocalDateTime.now();
        return p;
    }
}
