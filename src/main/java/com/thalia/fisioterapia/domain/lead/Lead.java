package com.thalia.fisioterapia.domain.lead;

import jakarta.persistence.*;
import lombok.Getter;
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

    public void registrarContato() {
        validarEstado(LeadStatus.NOVO);
        this.status = LeadStatus.CONTATADO;
    }

    public void marcarComoAgendado() {
        // só pode agendar depois de contatado
        validarEstado(LeadStatus.CONTATADO);
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
