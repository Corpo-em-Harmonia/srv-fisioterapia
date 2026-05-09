package com.thalia.fisioterapia.domain.lead;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Slf4j
@Document(collection = "leads")
@Getter
@Setter
public class Lead {

    @Id
    private String id;

    private String nome;
    private String sobrenome;
    private String email;
    private String telefone;
    private String observacao;

    private LeadStatus status;

    private LocalDateTime criadoEm;
    private int totalFaltas = 0;
    private int totalComparecimentos = 0;

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
        log.info("Lead [{}] avançou para CONTATADO", id);
    }

    public void marcarComoAgendado() {
        if (this.status != LeadStatus.NOVO && this.status != LeadStatus.CONTATADO) {
            throw new IllegalStateException(
                    "Não é possível agendar. Estado atual: %s | Estados permitidos: NOVO ou CONTATADO".formatted(status)
            );
        }
        this.status = LeadStatus.AGENDADO;
        log.info("Lead [{}] marcado como AGENDADO", id);
    }

    public void marcarComoPerdido() {
        if (status == LeadStatus.PERDIDO) return;
        this.status = LeadStatus.PERDIDO;
        log.info("Lead [{}] marcado como PERDIDO", id);
    }

    private void validarEstado(LeadStatus esperado) {
        if (this.status != esperado) {
            throw new IllegalStateException(
                    "Ação inválida. Estado atual: %s | Esperado: %s".formatted(status, esperado)
            );
        }
    }

    public void incrementarFaltas() {
        this.totalFaltas++;
    }

    public void incrementarComparecimentos() {
        this.totalComparecimentos++;
    }
}
