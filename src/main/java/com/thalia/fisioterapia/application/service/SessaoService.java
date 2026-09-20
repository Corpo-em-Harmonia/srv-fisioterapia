package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.domain.sessao.EscopoRemarcacao;
import com.thalia.fisioterapia.domain.sessao.PerfilUsuario;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infrastructure.repository.avaliacao.AvaliacaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.domain.sessao.SessaoEvolucao;
import com.thalia.fisioterapia.web.dto.avaliacao.IniciarAvaliacaoResponse;
import com.thalia.fisioterapia.web.dto.sessao.RegistrarEvolucaoRequest;
import com.thalia.fisioterapia.web.dto.sessao.SessaoHistoricoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessaoService {

    private static final String USUARIO_SISTEMA = "sistema";
    private static final PerfilUsuario PERFIL_PADRAO = PerfilUsuario.RECEPCAO;

    private final SessaoRepository sessaoRepository;
    private final LeadRepository leadRepository;
    private final PacienteRepository pacienteRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;

    public SessaoService(SessaoRepository sessaoRepository,
                         LeadRepository leadRepository,
                         PacienteRepository pacienteRepository,
                         AvaliacaoRepository avaliacaoRepository,
                         @Lazy UsuarioService usuarioService) {
        this.sessaoRepository = sessaoRepository;
        this.leadRepository = leadRepository;
        this.pacienteRepository = pacienteRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.usuarioService = usuarioService;
    }

    public Sessao registrarEvolucao(String id, RegistrarEvolucaoRequest req) {
        Sessao sessao = getById(id);
        SessaoEvolucao evolucao = new SessaoEvolucao(
                req.observacoes(), req.nivelDor(), req.mobilidade(), req.exercicios()
        );
        sessao.registrarEvolucao(evolucao);
        return sessaoRepository.save(sessao);
    }

    public List<SessaoHistoricoResponse> getHistoricoPaciente(String pacienteId) {
        return sessaoRepository.findByPacienteIdOrderByDataHoraDesc(pacienteId)
                .stream()
                .map(s -> {
                    SessaoEvolucao ev = s.getEvolucao();
                    return new SessaoHistoricoResponse(
                            s.getId(),
                            s.getNumeroOcorrencia(),
                            s.getDataHora().toString(),
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
        paciente = pacienteRepository.save(paciente);

        sessao.setPaciente(paciente.getId());
        sessaoRepository.save(sessao);

        usuarioService.criarParaPacienteSeNaoExistir(
                paciente.getNome(), paciente.getSobrenome(), lead.getEmail(), lead.getTelefone()
        );

        Avaliacao avaliacao = Avaliacao.criarParaPaciente(paciente.getId());
        avaliacao = avaliacaoRepository.save(avaliacao);

        String nomeCompleto = paciente.getNome() +
                (paciente.getSobrenome() != null && !paciente.getSobrenome().isBlank()
                        ? " " + paciente.getSobrenome() : "");

        return new IniciarAvaliacaoResponse(paciente.getId(), avaliacao.getId(), nomeCompleto.trim());
    }

    public Sessao marcarCompareceuAvaliacao(String id) {
        Sessao s = getById(id);
        s.marcarComparecimentoAvaliacao();
        Sessao salva = sessaoRepository.save(s);
        incrementarComparecimentos(salva);
        return salva;
    }

    public Sessao marcarAvaliada(String id) {
        Sessao s = getById(id);
        s.marcarAvaliada();
        return sessaoRepository.save(s);
    }

    public List<Sessao> listarPorDia(LocalDate dia) {
        Instant start = dia.atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
        Instant end = dia.plusDays(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
        return sessaoRepository.findByDataHoraBetweenOrderByDataHoraAsc(start, end);
    }

    public List<Sessao> listarPendentes() {
        return sessaoRepository.findPendentes(Instant.now());
    }

    public List<Sessao> listarPorPeriodo(String periodo, List<SessaoStatus> statusFiltro) {
        LocalDate hoje = LocalDate.now();
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
                return listarPendentes();
            }
            case "todos" -> {
                start = Instant.EPOCH;
                end = LocalDate.now().plusYears(100).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
            }
            default -> throw new BusinessException("Período inválido: %s".formatted(periodo));
        }

        if (statusFiltro != null && !statusFiltro.isEmpty()) {
            return sessaoRepository.findByDataHoraBetweenAndStatusInOrderByDataHoraAsc(start, end, statusFiltro);
        }
        return sessaoRepository.findByDataHoraBetweenOrderByDataHoraAsc(start, end);
    }

    public Map<String, Object> obterEstatisticas() {
        LocalDate hoje = LocalDate.now();
        Instant inicioHoje = hoje.atStartOfDay(AgendaUtil.ZONE_SP).toInstant();
        Instant fimHoje = hoje.plusDays(1).atStartOfDay(AgendaUtil.ZONE_SP).toInstant();

        long hojeTotal = sessaoRepository.countByDataHoraBetween(inicioHoje, fimHoje);
        long pendentes = sessaoRepository.findPendentes(Instant.now()).size();
        long compareceu = sessaoRepository.countByStatus(SessaoStatus.COMPARECEU);
        long faltou = sessaoRepository.countByStatus(SessaoStatus.FALTOU);
        long total = sessaoRepository.count();

        List<Sessao> todasSessoes = sessaoRepository.findAll();

        long pessoasComFaltas = todasSessoes.stream()
                .filter(s -> s.getStatus() == SessaoStatus.FALTOU)
                .map(s -> s.getPacienteId() != null ? s.getPacienteId() : s.getLeadId())
                .filter(id -> id != null)
                .distinct()
                .count();

        long pessoasQueCompareceram = todasSessoes.stream()
                .filter(s -> s.getStatus() == SessaoStatus.COMPARECEU)
                .map(s -> s.getPacienteId() != null ? s.getPacienteId() : s.getLeadId())
                .filter(id -> id != null)
                .distinct()
                .count();

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("hoje", hojeTotal);
        resultado.put("pendentes", pendentes);
        resultado.put("compareceu", compareceu);
        resultado.put("faltou", faltou);
        resultado.put("total", total);
        resultado.put("pessoasComFaltas", pessoasComFaltas);
        resultado.put("pessoasQueCompareceram", pessoasQueCompareceram);
        return resultado;
    }

    public Sessao marcarCompareceu(String id) {
        Sessao s = getById(id);
        s.marcarComparecimento();
        Sessao salva = sessaoRepository.save(s);
        incrementarComparecimentos(salva);
        return salva;
    }

    public Sessao marcarFaltou(String id) {
        Sessao s = getById(id);
        s.marcarFaltou();
        Sessao salva = sessaoRepository.save(s);
        incrementarFaltas(salva);
        return salva;
    }

    public Sessao cancelar(String id) {
        Sessao s = getById(id);
        s.cancelar(null, USUARIO_SISTEMA, PERFIL_PADRAO);
        return sessaoRepository.save(s);
    }

    public RemarcacaoResultado remarcar(String id, Instant novaDataHora, String escopoRaw, String motivo) {
        Sessao sessaoBase = getById(id);
        EscopoRemarcacao escopo = parseEscopo(escopoRaw);
        AgendaUtil.validarJanela(novaDataHora);

        List<Sessao> sessoesAfetadas = resolverEscopoRemarcacao(sessaoBase, escopo);
        Duration deslocamento = Duration.between(sessaoBase.getDataHora(), novaDataHora);
        Set<String> idsAfetados = sessoesAfetadas.stream().map(Sessao::getId).collect(Collectors.toSet());

        Map<String, Instant> novosHorarios = new HashMap<>();
        for (Sessao sessao : sessoesAfetadas) {
            Instant destino = sessao.getId().equals(sessaoBase.getId())
                    ? novaDataHora
                    : sessao.getDataHora().plus(deslocamento);
            AgendaUtil.validarJanela(destino);
            validarConflitosAgenda(destino, sessao.getId(), idsAfetados);
            novosHorarios.put(sessao.getId(), destino);
        }

        for (Sessao sessao : sessoesAfetadas) {
            Instant destino = novosHorarios.get(sessao.getId());
            sessao.remarcar(destino, escopo.name().toLowerCase(), motivo, USUARIO_SISTEMA, PERFIL_PADRAO);
        }

        sessaoRepository.saveAll(sessoesAfetadas);

        return new RemarcacaoResultado(
                sessoesAfetadas.size(),
                sessaoBase.getSerieId(),
                escopo.name().toLowerCase()
        );
    }

    private void incrementarFaltas(Sessao sessao) {
        if (sessao.getPacienteId() != null) {
            pacienteRepository.findById(sessao.getPacienteId()).ifPresent(paciente -> {
                paciente.incrementarFaltas();
                pacienteRepository.save(paciente);
            });
        } else if (sessao.getLeadId() != null) {
            leadRepository.findById(sessao.getLeadId()).ifPresent(lead -> {
                lead.incrementarFaltas();
                leadRepository.save(lead);
            });
        }
    }

    private void incrementarComparecimentos(Sessao sessao) {
        if (sessao.getPacienteId() != null) {
            pacienteRepository.findById(sessao.getPacienteId()).ifPresent(paciente -> {
                paciente.incrementarComparecimentos();
                pacienteRepository.save(paciente);
            });
        } else if (sessao.getLeadId() != null) {
            leadRepository.findById(sessao.getLeadId()).ifPresent(lead -> {
                lead.incrementarComparecimentos();
                leadRepository.save(lead);
            });
        }
    }

    private EscopoRemarcacao parseEscopo(String escopoRaw) {
        try {
            return EscopoRemarcacao.fromNullable(escopoRaw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("escopo invalido: %s".formatted(escopoRaw));
        }
    }

    private List<Sessao> resolverEscopoRemarcacao(Sessao sessaoBase, EscopoRemarcacao escopo) {
        if (escopo == EscopoRemarcacao.SOMENTE_ESTA) {
            return List.of(sessaoBase);
        }

        if (sessaoBase.getSerieId() == null || sessaoBase.getSerieId().isBlank()) {
            throw new BusinessException("Sessao nao pertence a uma serie para escopo informado");
        }

        List<Sessao> serie = sessaoRepository.findBySerieIdOrderByNumeroOcorrenciaAsc(sessaoBase.getSerieId());
        if (escopo == EscopoRemarcacao.TODA_SERIE) {
            return serie;
        }

        int ocorrenciaAtual = sessaoBase.getNumeroOcorrencia() != null ? sessaoBase.getNumeroOcorrencia() : 1;
        return serie.stream()
                .filter(s -> (s.getNumeroOcorrencia() != null ? s.getNumeroOcorrencia() : 1) >= ocorrenciaAtual)
                .sorted(Comparator.comparing(s -> s.getNumeroOcorrencia() != null ? s.getNumeroOcorrencia() : 1))
                .toList();
    }

    private void validarConflitosAgenda(Instant dataHora, String sessaoAtualId, Set<String> idsDaMesmaOperacao) {
        List<Sessao> conflitos = sessaoRepository.findByDataHoraAndStatusIn(dataHora, AgendaUtil.STATUS_CONFLITO).stream()
                .filter(s -> !s.getId().equals(sessaoAtualId))
                .filter(s -> !idsDaMesmaOperacao.contains(s.getId()))
                .toList();

        if (conflitos.size() < AgendaUtil.MAX_POR_HORARIO) {
            return;
        }

        List<AgendaConflictException.ConflitoAgendaItem> itens = conflitos.stream()
                .map(s -> new AgendaConflictException.ConflitoAgendaItem(
                        s.getId(),
                        s.getDataHora(),
                        resolverNomePessoa(s)
                ))
                .toList();

        throw new AgendaConflictException("Ja existe sessao nesse horario", itens);
    }

    private String resolverNomePessoa(Sessao sessao) {
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
