package com.thalia.fisioterapia.domain.sessao;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Document(collection = "sessoes")
// Agenda do dia / conflitos de horário, histórico do paciente, séries e avaliações pendentes
@CompoundIndex(name = "dataHora_status", def = "{ 'dataHora': 1, 'status': 1 }")
@CompoundIndex(name = "pacienteId_dataHora", def = "{ 'pacienteId': 1, 'dataHora': 1 }")
@CompoundIndex(name = "serieId_ocorrencia", def = "{ 'serieId': 1, 'numeroOcorrencia': 1 }")
@CompoundIndex(name = "status_dataHora", def = "{ 'status': 1, 'dataHora': 1 }")
@CompoundIndex(name = "leadId", def = "{ 'leadId': 1 }")
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

    private static final String SISTEMA = "sistema";

    protected Sessao() {}

    public Sessao(String leadId, SessaoTipo tipo, Instant dataHora, String observacao) {
        this(leadId, tipo, dataHora, observacao, SISTEMA, PerfilUsuario.ADMIN);
    }

    public Sessao(String leadId, SessaoTipo tipo, Instant dataHora, String observacao,
                  String usuarioId, PerfilUsuario perfil) {
        this.leadId = leadId;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.status = SessaoStatus.MARCADA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
        this.observacao = observacao;
        this.alteracoes = new ArrayList<>();
        registrarAlteracao(SessaoAuditoriaAcao.CRIAR, null, null, usuarioId, perfil);
    }

    public Sessao(String pacienteId, String avaliacaoId, Instant dataHora, String observacao) {
        this(pacienteId, avaliacaoId, dataHora, observacao, SISTEMA, PerfilUsuario.ADMIN);
    }

    public Sessao(String pacienteId, String avaliacaoId, Instant dataHora, String observacao,
                  String usuarioId, PerfilUsuario perfil) {
        this.pacienteId = pacienteId;
        this.avaliacaoId = avaliacaoId;
        this.tipo = SessaoTipo.SESSAO;
        this.dataHora = dataHora;
        this.status = SessaoStatus.MARCADA;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
        this.observacao = observacao;
        this.alteracoes = new ArrayList<>();
        registrarAlteracao(SessaoAuditoriaAcao.CRIAR, null, null, usuarioId, perfil);
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
        remarcar(novaDataHora, null, null, SISTEMA, PerfilUsuario.ADMIN);
    }

    public void marcarComparecimentoAvaliacao() {
        marcarComparecimentoAvaliacao(SISTEMA, PerfilUsuario.ADMIN);
    }

    public void marcarComparecimentoAvaliacao(String usuarioId, PerfilUsuario perfil) {
        validarPodeAgendar();
        if (this.tipo != SessaoTipo.AVALIACAO) {
            throw new IllegalStateException("Apenas avaliações podem aguardar fisioterapeuta.");
        }
        this.status = SessaoStatus.AGUARDANDO_AVALIACAO;
        this.atualizadoEm = Instant.now();
        registrarAlteracao(SessaoAuditoriaAcao.CONFIRMAR_CHEGADA_AVALIACAO, null, null, usuarioId, perfil);
    }

    public void marcarAvaliada() {
        marcarAvaliada(SISTEMA, PerfilUsuario.ADMIN);
    }

    public void marcarAvaliada(String usuarioId, PerfilUsuario perfil) {
        if (this.status != SessaoStatus.AGUARDANDO_AVALIACAO) {
            throw new IllegalStateException("Avaliação precisa estar aguardando para ser concluída.");
        }
        this.status = SessaoStatus.AVALIADA;
        this.atualizadoEm = Instant.now();
        registrarAlteracao(SessaoAuditoriaAcao.CONCLUIR_AVALIACAO, null, null, usuarioId, perfil);
    }

    public void marcarComparecimento() {
        marcarComparecimento(SISTEMA, PerfilUsuario.ADMIN);
    }

    public void marcarComparecimento(String usuarioId, PerfilUsuario perfil) {
        validarPodeAgendar();
        this.status = SessaoStatus.COMPARECEU;
        this.atualizadoEm = Instant.now();
        registrarAlteracao(SessaoAuditoriaAcao.REGISTRAR_COMPARECIMENTO, null, null, usuarioId, perfil);
    }

    public void marcarFaltou() {
        marcarFaltou(SISTEMA, PerfilUsuario.ADMIN);
    }

    public void marcarFaltou(String usuarioId, PerfilUsuario perfil) {
        validarPodeAgendar();
        this.status = SessaoStatus.FALTOU;
        this.atualizadoEm = Instant.now();
        registrarAlteracao(SessaoAuditoriaAcao.REGISTRAR_FALTA, null, null, usuarioId, perfil);
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
        cancelar(null, SISTEMA, PerfilUsuario.ADMIN);
    }

    public void registrarEvolucao(SessaoEvolucao evolucao) {
        registrarEvolucao(evolucao, SISTEMA, PerfilUsuario.ADMIN);
    }

    public void registrarEvolucao(SessaoEvolucao evolucao, String usuarioId, PerfilUsuario perfil) {
        validarPodeAgendar();
        this.evolucao = evolucao;
        this.status = SessaoStatus.REALIZADA;
        this.atualizadoEm = Instant.now();
        registrarAlteracao(SessaoAuditoriaAcao.REGISTRAR_EVOLUCAO, null, null, usuarioId, perfil);
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
