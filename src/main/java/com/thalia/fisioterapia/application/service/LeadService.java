package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.lead.LeadAcao;
import com.thalia.fisioterapia.domain.lead.LeadStatus;
import com.thalia.fisioterapia.domain.sessao.ModoAgendamento;
import com.thalia.fisioterapia.domain.sessao.PerfilUsuario;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoTipo;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.security.UsuarioLogado;
import com.thalia.fisioterapia.web.dto.agenda.AgendarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.agenda.AgendarAvaliacaoResponse;
import com.thalia.fisioterapia.web.dto.lead.CriarLeadRequest;
import com.thalia.fisioterapia.web.dto.lead.LeadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final SessaoRepository sessaoRepository;
    private final AgendaGuard agendaGuard;
    private final UsuarioLogado usuarioLogado;

    public LeadService(
            LeadRepository leadRepository,
            SessaoRepository sessaoRepository,
            AgendaGuard agendaGuard,
            UsuarioLogado usuarioLogado
    ) {
        this.leadRepository = leadRepository;
        this.sessaoRepository = sessaoRepository;
        this.agendaGuard = agendaGuard;
        this.usuarioLogado = usuarioLogado;
    }

    public LeadResponse criar(CriarLeadRequest request) {
        if (request.getEmail() != null && leadRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Lead já existe");
        }

        Lead lead = new Lead(
                request.getNome(),
                request.getSobrenome(),
                request.getEmail(),
                request.getTelefone(),
                request.getObservacao()
        );
        Lead saved = leadRepository.save(lead);
        log.info("Lead criado [id={}] email={}", saved.getId(), saved.getEmail());
        return toResponse(saved);
    }

    public Page<LeadResponse> listarTodos(Pageable pageable) {
        return leadRepository.findAll(pageable).map(this::toResponse);
    }

    public java.util.Optional<LeadResponse> buscarPorEmail(String email) {
        return leadRepository.findByEmail(email).map(this::toResponse);
    }

    public Page<LeadResponse> listarAtivos(Pageable pageable) {
        return leadRepository.findByStatusIn(List.of(LeadStatus.NOVO, LeadStatus.CONTATADO), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public LeadResponse executarAcaoSimples(String leadId, LeadAcao acao) {
        Lead lead = buscarLead(leadId);
        log.debug("Executando ação {} para lead [{}]", acao, leadId);
        switch (acao) {
            case REGISTRAR_CONTATO -> lead.registrarContato();
            case AGENDAR_AVALIACAO -> lead.marcarComoAgendado();
            case CANCELAR -> lead.marcarComoPerdido();
        }
        return toResponse(leadRepository.save(lead));
    }

    public AgendarAvaliacaoResponse agendarAvaliacao(String leadId, AgendarAvaliacaoRequest req) {
        log.info("Agendando avaliação para lead [{}] modo={}", leadId, req.modoAgendamento());
        String usuarioId = usuarioLogado.usuarioId();
        PerfilUsuario perfil = usuarioLogado.perfilAuditoria();
        return agendaGuard.executar(() -> agendarAvaliacao(leadId, req, usuarioId, perfil));
    }

    private AgendarAvaliacaoResponse agendarAvaliacao(String leadId, AgendarAvaliacaoRequest req,
                                                      String usuarioId, PerfilUsuario perfil) {
        Lead lead = buscarLead(leadId);
        ModoAgendamento modoAgendamento = parseModoAgendamento(req.modoAgendamento());

        LocalDateTime primeiraDataHora = req.dataHora();
        AgendaUtil.validarJanela(primeiraDataHora);

        int frequenciaSemanal = 1;
        int quantidadeSessoes = 1;
        List<LocalDateTime> datasAgendamento;

        if (modoAgendamento == ModoAgendamento.RECORRENTE) {
            if (req.frequenciaSemanal() == null) {
                throw new BusinessException("frequenciaSemanal obrigatorio para modo recorrente");
            }
            if (req.quantidadeSessoes() == null) {
                throw new BusinessException("quantidadeSessoes obrigatorio para modo recorrente");
            }
            frequenciaSemanal = req.frequenciaSemanal();
            quantidadeSessoes = req.quantidadeSessoes();

            int validadeGuiaDias = req.validadeGuiaDias() != null
                    ? req.validadeGuiaDias()
                    : AgendaUtil.VALIDADE_GUIA_PADRAO_DIAS;
            AgendaUtil.validarPlano(quantidadeSessoes, frequenciaSemanal, validadeGuiaDias);

            datasAgendamento = AgendaUtil.gerarDatas(primeiraDataHora, quantidadeSessoes, frequenciaSemanal, Set.of());
        } else {
            datasAgendamento = List.of(primeiraDataHora);
        }

        String serieId = modoAgendamento == ModoAgendamento.RECORRENTE
                ? "sr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                : null;

        List<Sessao> sessoesParaSalvar = new ArrayList<>();
        for (int i = 0; i < datasAgendamento.size(); i++) {
            LocalDateTime dataHoraOcorrencia = datasAgendamento.get(i);
            AgendaUtil.validarJanela(dataHoraOcorrencia);

            Instant dataHoraInstant = dataHoraOcorrencia.atZone(AgendaUtil.ZONE_SP).toInstant();
            agendaGuard.validarHorarioLivre(dataHoraInstant, req.fisioterapeutaId(), Set.of());

            SessaoTipo tipoSessao = (i == 0) ? SessaoTipo.AVALIACAO : SessaoTipo.SESSAO;
            Sessao sessao = new Sessao(lead.getId(), tipoSessao, dataHoraInstant, req.observacao(), usuarioId, perfil);

            if (serieId != null) sessao.definirSerie(serieId, i + 1);
            if (req.fisioterapeutaId() != null && !req.fisioterapeutaId().isBlank()) {
                sessao.atribuirFisioterapeuta(req.fisioterapeutaId());
            }
            sessoesParaSalvar.add(sessao);
        }

        lead.marcarComoAgendado();
        leadRepository.save(lead);
        List<Sessao> salvas = sessaoRepository.saveAll(sessoesParaSalvar);

        return new AgendarAvaliacaoResponse(
                modoAgendamento.name().toLowerCase(),
                serieId,
                salvas.size(),
                salvas.getFirst().getId(),
                "Agendamento criado com sucesso"
        );
    }

    private ModoAgendamento parseModoAgendamento(String modoAgendamento) {
        try {
            return ModoAgendamento.fromNullable(modoAgendamento);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("modoAgendamento invalido: %s".formatted(modoAgendamento));
        }
    }

    private Lead buscarLead(String id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado"));
    }

    private LeadResponse toResponse(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getNome(),
                lead.getSobrenome(),
                lead.getTelefone(),
                lead.getEmail(),
                lead.getObservacao(),
                lead.getStatus().name().toLowerCase(),
                lead.getCriadoEm()
        );
    }

    public void deletarLead(String id) {
        if (!leadRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lead não encontrado");
        }
        leadRepository.deleteById(id);
    }
}
