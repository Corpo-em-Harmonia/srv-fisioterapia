package com.thalia.fisioterapia.domain.lead;


import com.thalia.fisioterapia.domain.paciente.Paciente;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Entity
@Document(collection = "leads")
@Getter
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nome;
    private String sobrenome;
    private String email;
    private String telefone;

    @Enumerated(EnumType.STRING)
    private LeadStatus status;

    private Date criadoEm;

    protected Lead() {}

    public Lead(String nome, String sobrenome, String email, String telefone) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.telefone = telefone;
        this.status = LeadStatus.NOVO;
        this.criadoEm = new Date();
    }

    // =========================
    // REGRAS DE NEGÓCIO
    // =========================

    public void registrarContato() {
        validarEstado(LeadStatus.NOVO);
        this.status = LeadStatus.CONTATADO;
    }

    public void agendarAvaliacao() {
        validarEstado(LeadStatus.CONTATADO);
        this.status = LeadStatus.AGENDADO;
    }

    public void marcarComoPerdido() {
        if (status == LeadStatus.CONVERTIDO)
            throw new IllegalStateException("Lead já convertido");

        this.status = LeadStatus.PERDIDO;
    }

    public Paciente confirmarComparecimento() {
        validarEstado(LeadStatus.AGENDADO);

        this.status = LeadStatus.CONVERTIDO;

        return Paciente.fromLead(this);
    }


    private void validarEstado(LeadStatus esperado) {
        if (this.status != esperado) {
            throw new IllegalStateException(
                    "Ação inválida. Estado atual: " + status + " | Esperado: " + esperado
            );
        }
    }
}
