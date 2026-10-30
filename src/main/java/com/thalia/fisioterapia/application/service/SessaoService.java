package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.domain.sessao.EscopoRemarcacao;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infrastructure.repository.avaliacao.AvaliacaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.domain.sessao.SessaoEvolucao;
import com.thalia.fisioterapia.security.UsuarioLogado;
import com.thalia.fisioterapia.web.dto.avaliacao.IniciarAvaliacaoResponse;
import com.thalia.fisioterapia.web.dto.sessao.DisponibilidadeResponse;
import com.thalia.fisioterapia.web.dto.sessao.RegistrarEvolucaoRequest;
import com.thalia.fisioterapia.web.dto.sessao.SessaoEstatisticasResponse;
import com.thalia.fisioterapia.web.dto.sessao.SessaoHistoricoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final LeadRepository leadRepository;
    private final PacienteRepository pacienteRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;
    private final MongoTemplate mongoTemplate;
    private final AgendaGuard agendaGuard;
    private final UsuarioLogado usuarioLogado;

    public SessaoService(SessaoRepository sessaoRepository,
                         LeadRepository leadRepository,
                         PacienteRepository pacienteRepository,
                         AvaliacaoRepository avaliacaoRepository,
                         @Lazy UsuarioService usuarioService,
                         MongoTemplate mongoTemplate,
                         AgendaGuard agendaGuard,
                         UsuarioLogado usuarioLogado) {
        this.sessaoRepository = sessaoRepository;
        this.leadRepository = leadRepository;
        this.pacienteRepository = pacienteRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.usuarioService = usuarioService;
        this.mongoTemplate = mongoTemplate;
        this.agendaGuard = agendaGuard;
        this.usuarioLogado = usuarioLogado;
    }

    public Sessao registrarEvolucao(String id, RegistrarEvolucaoRequest req) {
        Sessao sessao = getById(id);
        SessaoEvolucao evolucao = new SessaoEvolucao(
                req.observacoes(), req.nivelDor(), req.mobilidade(), req.exercicios()
        );
        sessao.registrarEvolucao(evolucao, usuarioLogado.usuarioId(), usuarioLogado.perfilAuditoria());
        return sessaoRepository.save(sessao);
    }

    public List<SessaoHistoricoResponse> getHistoricoPaciente(String pacienteId) {
        usuarioLogado.garantirAcessoAoPaciente(pacienteId);
        return sessaoRepository.findByPacienteIdOrderByDataHoraDesc(pacienteId)
                .stream()
                .map(s -> {
                    SessaoEvolucao ev = s.getEvolucao();
                    return new SessaoHistoricoResponse(
                            s.getId(),
                            s.getNumeroOcorrencia(),
                            s.getDataHora().atZone(AgendaUtil.ZONE_SP).toLocalDateTime().toString(),
                            s.getStatus().name().toLowerCase(),
                            s.getTipo().name().toLowerCase(),
                            ev != null ? ev.getObservacoes() : null,
                            ev != null ? ev.getNivelDor() : null,
                            ev != null ? ev.getMobilidade() : null,
                            ev != null ? ev.getExercicios() : List.of(),
                            s.getAvaliacaoId()
                    );
                })
                .toList();
    }

    @Transactional
    public IniciarAvaliacaoResponse converterLeadParaPaciente(String sessaoId) {
        Sessao sessao = getById(sessaoId);

        if (sessao.getLeadId() == null) {
            throw new BusinessException("Sessão já está vinculada a um paciente");
        }

        Lead lead = leadRepository.findById(sessao.getLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado"));

        Paciente paciente = Paciente.fromLead(lead);
        if (sessao.getFisioterapeutaId() != null) {
            paciente.atribuirFisioterapeuta(sessao.getFisioterapeutaId());
        }
        paciente = pacienteRepository.save(paciente);

        sessao.setPaciente(paciente.getId());
        sessaoRepository.save(sessao);

        usuarioService.criarParaPacienteSeNaoExistir(
                paciente.getNome(), paciente.getSobrenome(), lead.getEmail(), lead.getTelefone()
        );

        Avaliacao avaliacao = Avaliacao.criarParaPaciente(paciente.getId());
        avaliacao = avaliacaoRepository.save(avaliacao);

        sessao.definirAvaliacao(avaliacao.getId());
        sessaoRepository.save(sessao);

        String nomeCompleto = paciente.getNome() +
                (paciente.getSobrenome() != null && !paciente.getSobrenome().isBlank()
                        ? " " + paciente.getSobrenome() : "");

        return new IniciarAvaliacaoResponse(paciente.getId(), avaliacao.getId(), nomeCompleto.trim());
    }

    public Sessao marcarCompareceuAvaliacao(String id) {
        Sessao s = getById(id);
        s.marcarComparecimentoAvaliacao(usuarioLogado.usuarioId(), usuarioLogado.perfilAuditoria());
        Sessao salva = sessaoRepository.save(s);
        incrementarContador(salva, "totalComparecimentos");
        return salva;
    }

    public Sessao marcarAvaliada(String id) {
        Sessao s = getById(id);
        s.marcarAvaliada(usuarioLogado.usuarioId(), usuarioLogado.perfilAuditoria());
        return sessaoRepository.save(s);
    }

    /** Agenda do paciente logado: o paciente nunca recebe sessões de outras pessoas. */
    public Page<Sessao> listarDoPacienteLogado(Pageable pageable) {
        return usuarioLogado.pacienteId()
                .map(pacienteId -> sessaoRepository.findByPacienteId(pacienteId, pageable))
                .orElseGet(() -> Page.empty(pageable));
    }

    public Page<Sessao> listarPorDia(LocalDate dia, List<SessaoStatus> statusFiltro, Pageable pageable) {
        Instant start = dia.atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
        Instant end = dia.plusDays(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
        if (statusFiltro != null && !statusFiltro.isEmpty()) {
            return sessaoRepository.findByDataHoraBetweenAndStatusIn(start, end, statusFiltro, pageable);
        }
        return sessaoRepository.findByDataHoraBetween(start, end, pageable);
    }

    public Page<Sessao> listarPendentes(List<SessaoStatus> statusFiltro, Pageable pageable) {
        if (statusFiltro != null && !statusFiltro.isEmpty()) {
            return sessaoRepository.findPendentesComStatus(Instant.now(), statusFiltro, pageable);
        }
        return sessaoRepository.findPendentes(Instant.now(), pageable);
    }

    public Page<Sessao> listarPorPeriodo(String periodo, List<SessaoStatus> statusFiltro, Pageable pageable) {
        LocalDate hoje = LocalDate.now(AgendaUtil.ZONE_SP);
        Instant start, end;

        switch (periodo.toLowerCase()) {
            case "hoje" -> {
                start = hoje.atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
                end = hoje.plusDays(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
            }
            case "semana" -> {
                start = hoje.with(DayOfWeek.MONDAY).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
                end = hoje.with(DayOfWeek.SUNDAY).plusDays(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
            }
            case "mes" -> {
                start = hoje.withDayOfMonth(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
                end = hoje.plusMonths(1).withDayOfMonth(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
            }
            case "pendentes" -> {
                return listarPendentes(statusFiltro, pageable);
            }
            case "todos" -> {
                start = Instant.EPOCH;
                end = hoje.plusYears(100).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
            }
            default -> throw new BusinessException("Período inválido: %s".formatted(periodo));
        }

        if (statusFiltro != null && !statusFiltro.isEmpty()) {
            return sessaoRepository.findByDataHoraBetweenAndStatusIn(start, end, statusFiltro, pageable);
        }
        return sessaoRepository.findByDataHoraBetween(start, end, pageable);
    }

    /**
     * Horários de 30 em 30 min dentro da janela de atendimento real (AgendaUtil), sem pausa de
     * almoço — antes esse cálculo era duplicado em AgendamentoController com uma janela (08h–19h
     * + pausa de almoço) diferente da que validarJanela realmente aplica (08h–20h, sem almoço),
     * então um horário podia ser aceito ao agendar mas nunca aparecer como disponível.
     */
    private static final List<LocalTime> HORARIOS_ATENDIMENTO = gerarHorariosAtendimento();

    private static List<LocalTime> gerarHorariosAtendimento() {
        List<LocalTime> horarios = new ArrayList<>();
        LocalTime cursor = AgendaUtil.INICIO_ATENDIMENTO;
        while (cursor.isBefore(AgendaUtil.FIM_ATENDIMENTO)) {
            horarios.add(cursor);
            cursor = cursor.plusMinutes(30);
        }
        return List.copyOf(horarios);
    }

    public List<DisponibilidadeResponse> consultarDisponibilidade(LocalDate date, String excludeId, String fisioterapeutaId) {
        DayOfWeek diaSemana = date.getDayOfWeek();
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return List.of();
        }

        Instant inicioDia = date.atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
        Instant fimDia = date.plusDays(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();

        // Uma única consulta pro dia inteiro em vez de uma por horário (eram ~22 round-trips).
        Map<Instant, List<Sessao>> sessoesPorHorario = sessaoRepository
                .findByDataHoraBetweenAndStatusInOrderByDataHoraAsc(inicioDia, fimDia, AgendaUtil.STATUS_CONFLITO)
                .stream()
                .filter(s -> excludeId == null || !excludeId.equals(s.getId()))
                .collect(Collectors.groupingBy(Sessao::getDataHora));

        List<DisponibilidadeResponse> resposta = new ArrayList<>();
        for (LocalTime h : HORARIOS_ATENDIMENTO) {
            Instant dataHora = ZonedDateTime.of(date, h, AgendaUtil.ZONE_SP).toInstant();
            List<Sessao> sessoesNoHorario = sessoesPorHorario.getOrDefault(dataHora, List.of());

            boolean disponivelGlobal = sessoesNoHorario.size() < AgendaUtil.MAX_POR_HORARIO;
            boolean disponivelFisio = fisioterapeutaId == null
                    || sessoesNoHorario.stream().noneMatch(s -> fisioterapeutaId.equals(s.getFisioterapeutaId()));

            resposta.add(new DisponibilidadeResponse(
                    "%02d:%02d".formatted(h.getHour(), h.getMinute()),
                    disponivelGlobal && disponivelFisio
            ));
        }
        return resposta;
    }

    public SessaoEstatisticasResponse obterEstatisticas() {
        LocalDate hoje = LocalDate.now(AgendaUtil.ZONE_SP);
        Instant inicioHoje = hoje.atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
        Instant fimHoje = hoje.plusDays(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();

        long hojeTotal = sessaoRepository.countByDataHoraBetween(inicioHoje, fimHoje);
        long pendentes = sessaoRepository.countPendentes(Instant.now());
        long compareceu = sessaoRepository.countByStatus(SessaoStatus.COMPARECEU);
        long faltou = sessaoRepository.countByStatus(SessaoStatus.FALTOU);
        long total = sessaoRepository.count();

        long pessoasComFaltas = contarPessoasDistintas(SessaoStatus.FALTOU);
        long pessoasQueCompareceram = contarPessoasDistintas(SessaoStatus.COMPARECEU);

        return new SessaoEstatisticasResponse(
            hojeTotal,
            pendentes,
            compareceu,
            faltou,
            total,
            pessoasComFaltas,
            pessoasQueCompareceram
        );
    }

    public Sessao marcarCompareceu(String id) {
        Sessao s = getById(id);
        s.marcarComparecimento(usuarioLogado.usuarioId(), usuarioLogado.perfilAuditoria());
        Sessao salva = sessaoRepository.save(s);
        incrementarContador(salva, "totalComparecimentos");
        return salva;
    }

    public Sessao marcarFaltou(String id) {
        Sessao s = getById(id);
        s.marcarFaltou(usuarioLogado.usuarioId(), usuarioLogado.perfilAuditoria());
        Sessao salva = sessaoRepository.save(s);
        incrementarContador(salva, "totalFaltas");
        return salva;
    }

    public Sessao cancelar(String id) {
        Sessao s = getById(id);
        s.cancelar(null, usuarioLogado.usuarioId(), usuarioLogado.perfilAuditoria());
        return sessaoRepository.save(s);
    }

    public RemarcacaoResultado remarcar(String id, Instant novaDataHora, String escopoRaw, String motivo) {
        EscopoRemarcacao escopo = parseEscopo(escopoRaw);
        AgendaUtil.validarJanela(novaDataHora);
        String usuarioId = usuarioLogado.usuarioId();
        var perfil = usuarioLogado.perfilAuditoria();

        return agendaGuard.executar(() -> {
            Sessao sessaoBase = getById(id);
            List<Sessao> sessoesAfetadas = resolverEscopoRemarcacao(sessaoBase, escopo);
            Duration deslocamento = Duration.between(sessaoBase.getDataHora(), novaDataHora);
            Set<String> idsAfetados = sessoesAfetadas.stream().map(Sessao::getId).collect(Collectors.toSet());

            Map<String, Instant> novosHorarios = new HashMap<>();
            for (Sessao sessao : sessoesAfetadas) {
                Instant destino = sessao.getId().equals(sessaoBase.getId())
                        ? novaDataHora
                        : sessao.getDataHora().plus(deslocamento);
                AgendaUtil.validarJanela(destino);
                agendaGuard.validarHorarioLivre(destino, sessao.getFisioterapeutaId(), idsAfetados);
                novosHorarios.put(sessao.getId(), destino);
            }

            for (Sessao sessao : sessoesAfetadas) {
                Instant destino = novosHorarios.get(sessao.getId());
                sessao.remarcar(destino, escopo.name().toLowerCase(), motivo, usuarioId, perfil);
            }

            sessaoRepository.saveAll(sessoesAfetadas);

            return new RemarcacaoResultado(
                    sessoesAfetadas.size(),
                    sessaoBase.getSerieId(),
                    escopo.name().toLowerCase()
            );
        });
    }

    /**
     * Incremento atômico ($inc) no paciente ou lead da sessão. Antes era "lê, soma 1 e salva":
     * duas marcações ao mesmo tempo liam o mesmo valor e uma delas se perdia.
     */
    private void incrementarContador(Sessao sessao, String campo) {
        Update inc = new Update().inc(campo, 1);
        if (sessao.getPacienteId() != null) {
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(sessao.getPacienteId())), inc, Paciente.class);
        } else if (sessao.getLeadId() != null) {
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(sessao.getLeadId())), inc, Lead.class);
        }
    }

    /**
     * Pessoas distintas com sessões no status (o paciente, ou o lead quando a sessão ainda não tem
     * paciente). Usa distinct no banco em vez de trazer as sessões para a memória.
     */
    private long contarPessoasDistintas(SessaoStatus status) {
        Function<Criteria, Query> comStatus = filtro -> Query.query(
                new Criteria().andOperator(Criteria.where("status").is(status), filtro));
        long pacientes = mongoTemplate.findDistinct(
                comStatus.apply(Criteria.where("pacienteId").ne(null)),
                "pacienteId", Sessao.class, String.class).size();
        long leads = mongoTemplate.findDistinct(
                comStatus.apply(new Criteria().andOperator(
                        Criteria.where("pacienteId").is(null), Criteria.where("leadId").ne(null))),
                "leadId", Sessao.class, String.class).size();
        return pacientes + leads;
    }

    private EscopoRemarcacao parseEscopo(String escopoRaw) {
        try {
            return EscopoRemarcacao.fromNullable(escopoRaw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("escopo invalido: %s".formatted(escopoRaw));
        }
    }

    // Sessões nesses status já refletem um atendimento concluído (ou em avaliação) — remarcação
    // em lote (toda_serie/desta_em_diante) nunca deve tocar nelas, só nas que ainda não aconteceram.
    private static final Set<SessaoStatus> STATUS_REMARCAVEL_EM_LOTE =
            Set.of(SessaoStatus.MARCADA, SessaoStatus.REMARCADA);

    private List<Sessao> resolverEscopoRemarcacao(Sessao sessaoBase, EscopoRemarcacao escopo) {
        if (escopo == EscopoRemarcacao.SOMENTE_ESTA) {
            return List.of(sessaoBase);
        }

        if (sessaoBase.getSerieId() == null || sessaoBase.getSerieId().isBlank()) {
            throw new BusinessException("Sessao nao pertence a uma serie para escopo informado");
        }

        List<Sessao> serie = sessaoRepository.findBySerieIdOrderByNumeroOcorrenciaAsc(sessaoBase.getSerieId());

        if (escopo == EscopoRemarcacao.TODA_SERIE) {
            return serie.stream().filter(this::remarcavelEmLote).toList();
        }

        int ocorrenciaAtual = sessaoBase.getNumeroOcorrencia() != null ? sessaoBase.getNumeroOcorrencia() : 1;
        return serie.stream()
                .filter(s -> (s.getNumeroOcorrencia() != null ? s.getNumeroOcorrencia() : 1) >= ocorrenciaAtual)
                .filter(this::remarcavelEmLote)
                .sorted(Comparator.comparing(s -> s.getNumeroOcorrencia() != null ? s.getNumeroOcorrencia() : 1))
                .toList();
    }

    private boolean remarcavelEmLote(Sessao s) {
        return STATUS_REMARCAVEL_EM_LOTE.contains(s.getStatus());
    }

    private Sessao getById(String id) {
        return sessaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada: " + id));
    }

    public record RemarcacaoResultado(
            int sessoesAfetadas,
            String serieId,
            String escopoAplicado
    ) {}
}
