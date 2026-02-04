package com.thalia.fisioterapia.domain.lead;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String nome;
    private String sobrenome;
    private String email;
    private String telefone;
    private Date date;
    private LeadStatus status;


    public Lead(String nome, String sobrenome, String email, String telefone) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.telefone = telefone;
        this.status = LeadStatus.NOVO;
        this.date = new Date();
    }

    public Lead(
            String id,
            String nome,
            String sobrenome,
            String email,
            String telefone,
            LeadStatus status,
            Date date
    ) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.telefone = telefone;
        this.status = status;
        this.date = new Date();
    }

    public void iniciarAvaliacao() {
        if (this.status != LeadStatus.NOVO) {
            throw new IllegalStateException("Lead não pode iniciar avaliação");
        }
        this.status = LeadStatus.AVALIACAO_INICIADA;
    }

    public void finalizarAvaliacao() {
        if (status != LeadStatus.AVALIACAO_INICIADA) {
            throw new IllegalStateException("Avaliação não iniciada");
        }
        this.status = LeadStatus.PACIENTE;
    }


    @PrePersist
    protected void onCreate() {
        this.date = new Date();
    }

    public void marcarComoConvertido() {
        this.status = LeadStatus.AVALIACAO_INICIADA;
    }
}
