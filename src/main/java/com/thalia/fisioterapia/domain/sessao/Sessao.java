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

    private String leadId;        // Avaliação: preenchido
    private String pacienteId;    // NULL até converter
    private String avaliacaoId;   // ✅ Sessões recorrentes: ID da avaliação que gerou


    private SessaoTipo tipo;
    private Instant dataHora;
    private SessaoStatus status;

    private Instant criadoEm;
    private Instant atualizadoEm;
    private String observacao;



    protected Sessao() {}

    public Sessao(String leadId, SessaoTipo tipo, Instant dataHora, String observacao) {
        this.leadId = leadId;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.status = SessaoStatus.MARCADA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
        this.observacao = observacao;
    }
    public Sessao(String pacienteId, String avaliacaoId, Instant dataHora, String observacao) {
        this.pacienteId = pacienteId;
        this.avaliacaoId = avaliacaoId;
        this.tipo = SessaoTipo.SESSAO;
        this.dataHora = dataHora;
        this.status = SessaoStatus.MARCADA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
        this.observacao = observacao;
    }

    public void setPaciente(String pacienteId) {
        this.pacienteId = pacienteId;
        this.atualizadoEm = Instant.now();
    }

    public void remarcar(Instant novaDataHora) {
        validarNaoCancelada(); // ✅ Só cancela bloqueia
        this.dataHora = novaDataHora;
        this.status = SessaoStatus.REMARCADA;
        this.atualizadoEm = Instant.now();
    }

    public void marcarComparecimentoAvaliacao() {
        validarNaoCancelada();
        if (this.tipo != SessaoTipo.AVALIACAO) {
            throw new IllegalStateException("Apenas avaliações podem aguardar fisioterapeuta.");
        }
        this.status = SessaoStatus.AGUARDANDO_AVALIACAO;
        this.atualizadoEm = Instant.now();
    }

    // ✅ NOVO: Fisio marca que fez a avaliação
    public void marcarAvaliada() {
        validarNaoCancelada();
        if (this.status != SessaoStatus.AGUARDANDO_AVALIACAO) {
            throw new IllegalStateException("Avaliação precisa estar aguardando para ser concluída.");
        }
        this.status = SessaoStatus.AVALIADA;
        this.atualizadoEm = Instant.now();
    }

    public void marcarComparecimento() {
        validarNaoCancelada(); // ✅ Só cancela bloqueia
        this.status = SessaoStatus.COMPARECEU;
        this.atualizadoEm = Instant.now();
    }

    public void marcarFaltou() {
        validarNaoCancelada(); // ✅ Só cancela bloqueia
        this.status = SessaoStatus.FALTOU;
        this.atualizadoEm = Instant.now();
    }

    public void cancelar() {
        this.status = SessaoStatus.CANCELADA;
        this.atualizadoEm = Instant.now();
    }

    // ✅ APENAS cancelada bloqueia (conforme acordado)
    private void validarNaoCancelada() {
        if (this.status == SessaoStatus.CANCELADA) {
            throw new IllegalStateException("Sessão cancelada não pode receber ações.");
        }
    }
}
