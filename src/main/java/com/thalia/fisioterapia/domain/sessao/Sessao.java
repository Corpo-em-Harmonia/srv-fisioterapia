package com.thalia.fisioterapia.domain.sessao;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "sessoes")
public class Sessao {

    @Id
    private String id;

    // ✅ Enquanto não virou paciente, a sessão aponta pro lead
    private String leadId;

    // ✅ Quando virar paciente (compareceu na avaliação), preenche
    private String pacienteId;

    private SessaoTipo tipo;        // AVALIACAO ou SESSAO
    private Instant dataHora;
    private SessaoStatus status;

    private Instant criadoEm;
    private Instant atualizadoEm;

    protected Sessao() {}

    public Sessao(String leadId, SessaoTipo tipo, Instant dataHora) {
        this.leadId = leadId;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.status = SessaoStatus.MARCADA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public void setPaciente(String pacienteId) {
        this.pacienteId = pacienteId;
        this.atualizadoEm = Instant.now();
    }

    public void remarcar(Instant novaDataHora) {
        validarNaoEncerrada();
        this.dataHora = novaDataHora;
        this.status = SessaoStatus.REMARCADA;
        this.atualizadoEm = Instant.now();
    }

    public void marcarComparecimento() {
        validarNaoEncerrada();
        this.status = SessaoStatus.COMPARECEU;
        this.atualizadoEm = Instant.now();
    }

    public void marcarFaltou() {
        validarNaoEncerrada();
        this.status = SessaoStatus.FALTOU;
        this.atualizadoEm = Instant.now();
    }

    public void cancelar() {
        this.status = SessaoStatus.CANCELADA;
        this.atualizadoEm = Instant.now();
    }

    private void validarNaoEncerrada() {
        if (this.status == SessaoStatus.CANCELADA) {
            throw new IllegalStateException("Sessão cancelada não pode receber ações.");
        }
        if (this.status == SessaoStatus.COMPARECEU) {
            throw new IllegalStateException("Sessão já concluída (compareceu).");
        }
    }
}
