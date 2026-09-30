package com.thalia.fisioterapia.domain.sessao;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Document(collection = "sessoes")
public class Sessao {

    @Id
    private String id;

    private String leadId;
    private String pacienteId;
    private String avaliacaoId;
    private String fisioterapeutaId;

    private SessaoTipo tipo;
    private Instant dataHora;
    private SessaoStatus status;

    private Instant criadoEm;
    private Instant atualizadoEm;
    private String observacao;
    private String serieId;
    private Integer numeroOcorrencia;
    private List<SessaoAlteracao> alteracoes;
    private SessaoEvolucao evolucao;

    protected Sessao() {}

    public Sessao(String leadId, SessaoTipo tipo, Instant dataHora, String observacao) {
        this.leadId = leadId;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.status = SessaoStatus.MARCADA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
        this.observacao = observacao;
        this.alteracoes = new ArrayList<>();
        registrarAlteracao(SessaoAuditoriaAcao.CRIAR, null, null, "sistema", PerfilUsuario.ADMIN);
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
        this.alteracoes = new ArrayList<>();
        registrarAlteracao(SessaoAuditoriaAcao.CRIAR, null, null, "sistema", PerfilUsuario.ADMIN);
    }

    public void definirSerie(String serieId, int numeroOcorrencia) {
        this.serieId = serieId;
        this.numeroOcorrencia = numeroOcorrencia;
        this.atualizadoEm = Instant.now();
    }

    public void setPaciente(String pacienteId) {
        this.pacienteId = pacienteId;
        this.atualizadoEm = Instant.now();
    }

    public void definirAvaliacao(String avaliacaoId) {
        this.avaliacaoId = avaliacaoId;
        this.atualizadoEm = Instant.now();
    }

    public void atribuirFisioterapeuta(String fisioterapeutaId) {
        this.fisioterapeutaId = fisioterapeutaId;
        this.atualizadoEm = Instant.now();
    }

    public void remarcar(Instant novaDataHora, String escopo, String motivo, String usuarioId, PerfilUsuario perfil) {
        validarPodeAgendar();
        this.dataHora = novaDataHora;
        this.status = SessaoStatus.REMARCADA;
        this.atualizadoEm = Instant.now();
        registrarAlteracao(SessaoAuditoriaAcao.REMARCAR, escopo, motivo, usuarioId, perfil);
    }

    public void remarcar(Instant novaDataHora) {
        remarcar(novaDataHora, null, null, "sistema", PerfilUsuario.ADMIN);
    }

    public void marcarComparecimentoAvaliacao() {
        validarPodeAgendar();
        if (this.tipo != SessaoTipo.AVALIACAO) {
            throw new IllegalStateException("Apenas avaliações podem aguardar fisioterapeuta.");
        }
        this.status = SessaoStatus.AGUARDANDO_AVALIACAO;
        this.atualizadoEm = Instant.now();
    }

    public void marcarAvaliada() {
        if (this.status != SessaoStatus.AGUARDANDO_AVALIACAO) {
            throw new IllegalStateException("Avaliação precisa estar aguardando para ser concluída.");
        }
        this.status = SessaoStatus.AVALIADA;
        this.atualizadoEm = Instant.now();
    }

    public void marcarComparecimento() {
        validarPodeAgendar();
        this.status = SessaoStatus.COMPARECEU;
        this.atualizadoEm = Instant.now();
    }

    public void marcarFaltou() {
        validarPodeAgendar();
        this.status = SessaoStatus.FALTOU;
        this.atualizadoEm = Instant.now();
    }

    public void cancelar(String motivo, String usuarioId, PerfilUsuario perfil) {
        if (this.status == SessaoStatus.CANCELADA) {
            return;
        }
        this.status = SessaoStatus.CANCELADA;
        this.atualizadoEm = Instant.now();
        registrarAlteracao(SessaoAuditoriaAcao.CANCELAR, null, motivo, usuarioId, perfil);
    }

    public void cancelar() {
        cancelar(null, "sistema", PerfilUsuario.ADMIN);
    }

    public void registrarEvolucao(SessaoEvolucao evolucao) {
        validarPodeAgendar();
        this.evolucao = evolucao;
        this.status = SessaoStatus.REALIZADA;
        this.atualizadoEm = Instant.now();
    }

    private static final Set<SessaoStatus> STATUS_EDITAVEIS = Set.of(SessaoStatus.MARCADA, SessaoStatus.REMARCADA);

    /** Só sessões ainda não "concluídas" (não compareceu/faltou/avaliada/realizada/cancelada) podem ser alteradas. */
    private void validarPodeAgendar() {
        if (this.status == SessaoStatus.CANCELADA) {
            throw new IllegalStateException("Sessão cancelada não pode receber ações.");
        }
        if (!STATUS_EDITAVEIS.contains(this.status)) {
            throw new IllegalStateException(
                    "Sessão com status %s não pode ser alterada.".formatted(this.status));
        }
    }

    private void registrarAlteracao(
            SessaoAuditoriaAcao acao,
            String escopo,
            String motivo,
            String usuarioId,
            PerfilUsuario perfil
    ) {
        if (this.alteracoes == null) {
            this.alteracoes = new ArrayList<>();
        }
        this.alteracoes.add(new SessaoAlteracao(
                acao,
                escopo,
                motivo,
                usuarioId,
                perfil,
                Instant.now()
        ));
    }
}
