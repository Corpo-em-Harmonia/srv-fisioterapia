package com.thalia.fisioterapia.domain.lead;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Entity
@Document(collection = "leads")
@Getter
@Setter
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nome;
    private String sobrenome;
    private String email;
    private String telefone;
    private String observacao;

    @Enumerated(EnumType.STRING)
    private LeadStatus status;

    private LocalDateTime criadoEm;

    protected Lead() {}

    public Lead(String nome, String sobrenome, String email, String telefone,String observacao) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.telefone = telefone;
        this.status = LeadStatus.NOVO;
        this.observacao = observacao;
        this.criadoEm = LocalDateTime.now();
    }

    public void registrarContato() {
        validarEstado(LeadStatus.NOVO);
        this.status = LeadStatus.CONTATADO;
    }

    public void marcarComoAgendado() {
        // pode agendar se for NOVO ou CONTATADO
        if (this.status != LeadStatus.NOVO && this.status != LeadStatus.CONTATADO) {
            throw new IllegalStateException(
                    "Não é possível agendar. Estado atual: " + status + " | Estados permitidos: NOVO ou CONTATADO"
            );
        }
        this.status = LeadStatus.AGENDADO;
    }

    public void marcarComoPerdido() {
        if (status == LeadStatus.PERDIDO) return;
        this.status = LeadStatus.PERDIDO;
    }

    private void validarEstado(LeadStatus esperado) {
        if (this.status != esperado) {
            throw new IllegalStateException(
                    "Ação inválida. Estado atual: " + status + " | Esperado: " + esperado
            );
        }
    }
}
