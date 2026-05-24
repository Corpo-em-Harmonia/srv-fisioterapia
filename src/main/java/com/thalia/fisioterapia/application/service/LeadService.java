package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.lead.LeadAcao;
import com.thalia.fisioterapia.domain.lead.LeadStatus;
import com.thalia.fisioterapia.domain.sessao.ModoAgendamento;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoTipo;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.web.dto.agenda.AgendarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.agenda.AgendarAvaliacaoResponse;
import com.thalia.fisioterapia.web.dto.lead.CriarLeadRequest;
import com.thalia.fisioterapia.web.dto.lead.LeadResponse;
import lombok.extern.slf4j.Slf4j;
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
    private final PacienteRepository pacienteRepository;

    public LeadService(
            LeadRepository leadRepository,
            SessaoRepository sessaoRepository,
            PacienteRepository pacienteRepository
    ) {
        this.leadRepository = leadRepository;
        this.sessaoRepository = sessaoRepository;
        this.pacienteRepository = pacienteRepository;
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

    public List<LeadResponse> listarTodos() {
        return leadRepository.findAll().stream().map(this::toResponse).toList();
    }

    public java.util.Optional<LeadResponse> buscarPorEmail(String email) {
        return leadRepository.findByEmail(email).map(this::toResponse);
    }

    public List<LeadResponse> listarAtivos() {
        return leadRepository.findByStatusIn(List.of(LeadStatus.NOVO, LeadStatus.CONTATADO))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public Lead executarAcaoSimples(String leadId, LeadAcao acao) {
        Lead lead = buscarLead(leadId);
        log.debug("Executando ação {} para lead [{}]", acao, leadId);
        switch (acao) {
            case REGISTRAR_CONTATO -> lead.registrarContato();
            case AGENDAR_AVALIACAO -> lead.marcarComoAgendado();
            case CANCELAR -> lead.marcarComoPerdido();
        }
        return leadRepository.save(lead);
    }

    @Transactional
    public AgendarAvaliacaoResponse agendarAvaliacao(String leadId, AgendarAvaliacaoRequest req) {
        log.info("Agendando avaliação para lead [{}] modo={}", leadId, req.modoAgendamento());
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
            validarConflitosAgenda(dataHoraInstant);

            SessaoTipo tipoSessao = (i == 0) ? SessaoTipo.AVALIACAO : SessaoTipo.SESSAO;
            Sessao sessao = new Sessao(lead.getId(), tipoSessao, dataHoraInstant, req.observacao());

            if (serieId != null) sessao.definirSerie(serieId, i + 1);
            sessoesParaSalvar.add(sessao);
        }

        lead.marcarComoAgendado();
        leadRepository.save(lead);
        List<Sessao> salvas = sessaoRepository.saveAll(sessoesParaSalvar);

        return new AgendarAvaliacaoResponse(
                modoAgendamento.name().toLowerCase(),
                serieId,
                salvas.size(),
                salvas.get(0).getId(),
                "Agendamento criado com sucesso"
        );
    }

    private void validarConflitosAgenda(Instant dataHora) {
        List<Sessao> conflitos = sessaoRepository.findByDataHoraAndStatusIn(dataHora, AgendaUtil.STATUS_CONFLITO);
        if (conflitos.size() >= AgendaUtil.MAX_POR_HORARIO) {
            List<AgendaConflictException.ConflitoAgendaItem> itens = conflitos.stream()
                    .map(s -> new AgendaConflictException.ConflitoAgendaItem(
                            s.getId(), s.getDataHora(), obterNomePessoa(s)))
                    .toList();
            throw new AgendaConflictException("Ja existe sessao nesse horario", itens);
        }
    }

    private String obterNomePessoa(Sessao sessao) {
        if (sessao.getPacienteId() != null) {
            return pacienteRepository.findById(sessao.getPacienteId())
                    .map(p -> p.getNome() != null ? p.getNome() : "Paciente")
                    .orElse("Paciente");
        }
        if (sessao.getLeadId() != null) {
            return leadRepository.findById(sessao.getLeadId())
                    .map(l -> l.getNome() != null ? l.getNome() : "Lead")
                    .orElse("Lead");
        }
        return "Paciente";
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
