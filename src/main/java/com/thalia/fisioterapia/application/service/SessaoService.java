package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.sessao.EscopoRemarcacao;
import com.thalia.fisioterapia.domain.sessao.PerfilUsuario;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SessaoService {

    private static final List<SessaoStatus> STATUS_CONFLITO = List.of(
            SessaoStatus.MARCADA,
            SessaoStatus.REMARCADA,
            SessaoStatus.AGUARDANDO_AVALIACAO
    );
    private static final String USUARIO_SISTEMA = "sistema";
    private static final PerfilUsuario PERFIL_PADRAO = PerfilUsuario.RECEPCAO;
    private static final LocalTime INICIO_ATENDIMENTO = LocalTime.of(8, 0);
    private static final LocalTime FIM_ATENDIMENTO = LocalTime.of(18, 0);

    private final SessaoRepository sessaoRepository;
    private final LeadRepository leadRepository;
    private final PacienteRepository pacienteRepository;

    public SessaoService(SessaoRepository sessaoRepository,
                         LeadRepository leadRepository,
                         PacienteRepository pacienteRepository) {
        this.sessaoRepository = sessaoRepository;
        this.leadRepository = leadRepository;
        this.pacienteRepository = pacienteRepository;
    }

    // NOVO: Recepção marca comparecimento de AVALIAÇÃO
    public Sessao marcarCompareceuAvaliacao(String id) {
        Sessao s = getById(id);
        s.marcarComparecimentoAvaliacao(); // Vai para AGUARDANDO_AVALIACAO
        Sessao salva = sessaoRepository.save(s);
        incrementarComparecimentos(salva);
        return salva;
    }

    //  NOVO: Fisio marca que fez a avaliação
    public Sessao marcarAvaliada(String id) {
        Sessao s = getById(id);
        s.marcarAvaliada(); // Vai para AVALIADA
        return sessaoRepository.save(s);
    }

    // Lista sessões de um dia específico
    public List<Sessao> listarPorDia(LocalDate dia) {
        Instant start = dia.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = dia.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return sessaoRepository.findByDataHoraBetweenOrderByDataHoraAsc(start, end);
    }

    // Lista sessões pendentes (hoje + atrasadas não confirmadas)
    public List<Sessao> listarPendentes() {
        Instant agora = Instant.now();
        return sessaoRepository.findPendentes(agora);
    }

    // Lista sessões por período (hoje, semana, mês, pendentes)
    public List<Sessao> listarPorPeriodo(String periodo, List<SessaoStatus> statusFiltro) {
        LocalDate hoje = LocalDate.now();
        Instant start, end;

        switch (periodo.toLowerCase()) {
            case "hoje" -> {
                start = hoje.atStartOfDay(ZoneId.systemDefault()).toInstant();
                end = hoje.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            }
            case "semana" -> {
                start = hoje.with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant();
                end = hoje.with(DayOfWeek.SUNDAY).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            }
            case "mes" -> {
                start = hoje.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                end = hoje.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            }
            case "pendentes" -> {
                return listarPendentes();
            }

            case "todos" -> {
                start = Instant.EPOCH;
                end = LocalDate.now().plusYears(100)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant();
            }

            default -> throw new BusinessException("Período inválido: " + periodo);
        }

        if (statusFiltro != null && !statusFiltro.isEmpty()) {
            return sessaoRepository.findByDataHoraBetweenAndStatusInOrderByDataHoraAsc(start, end, statusFiltro);
        }
        return sessaoRepository.findByDataHoraBetweenOrderByDataHoraAsc(start, end);
    }

    // Obter estatísticas completas (sessões + pessoas)
    public Map<String, Object> obterEstatisticas() {
        LocalDate hoje = LocalDate.now();
        Instant inicioHoje = hoje.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant fimHoje = hoje.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Estatísticas de sessões
        long hoje_total = sessaoRepository.countByDataHoraBetween(inicioHoje, fimHoje);
        long pendentes = sessaoRepository.findPendentes(Instant.now()).size();
        long compareceu = sessaoRepository.countByStatus(SessaoStatus.COMPARECEU);
        long faltou = sessaoRepository.countByStatus(SessaoStatus.FALTOU);
        long total = sessaoRepository.count();

        // Estatísticas de pessoas
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

        // Mescla tudo em um único Map
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("hoje", hoje_total);
        resultado.put("pendentes", pendentes);
        resultado.put("compareceu", compareceu);
        resultado.put("faltou", faltou);
        resultado.put("total", total);
        resultado.put("pessoasComFaltas", pessoasComFaltas);
        resultado.put("pessoasQueCompareceram", pessoasQueCompareceram);
        resultado.put("totalFaltas", faltou); // mesmo valor de faltou
        resultado.put("totalComparecimentos", compareceu); // mesmo valor de compareceu

        return resultado;
    }

    // Marca comparecimento e incrementa contador
    public Sessao marcarCompareceu(String id) {
        Sessao s = getById(id);
        s.marcarComparecimento();
        Sessao salva = sessaoRepository.save(s);
        incrementarComparecimentos(salva);
        return salva;
    }

    // Marca falta e incrementa contador
    public Sessao marcarFaltou(String id) {
        Sessao s = getById(id);
        s.marcarFaltou();
        Sessao salva = sessaoRepository.save(s);
        incrementarFaltas(salva);
        return salva;
    }

    // Cancela sessão
    public Sessao cancelar(String id) {
        Sessao s = getById(id);
        s.cancelar(null, USUARIO_SISTEMA, PERFIL_PADRAO);
        return sessaoRepository.save(s);
    }

    public RemarcacaoResultado remarcar(String id, Instant novaDataHora, String escopoRaw, String motivo) {
        Sessao sessaoBase = getById(id);
        EscopoRemarcacao escopo = parseEscopo(escopoRaw);
        validarJanelaAtendimento(novaDataHora);

        List<Sessao> sessoesAfetadas = resolverEscopoRemarcacao(sessaoBase, escopo);
        Duration deslocamento = Duration.between(sessaoBase.getDataHora(), novaDataHora);
        Set<String> idsAfetados = sessoesAfetadas.stream().map(Sessao::getId).collect(java.util.stream.Collectors.toSet());

        Map<String, Instant> novosHorarios = new HashMap<>();
        for (Sessao sessao : sessoesAfetadas) {
            Instant destino = sessao.getId().equals(sessaoBase.getId())
                    ? novaDataHora
                    : sessao.getDataHora().plus(deslocamento);
            validarJanelaAtendimento(destino);
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

    // Incrementa contador de faltas no Lead ou Paciente
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

    // Incrementa contador de comparecimentos no Lead ou Paciente
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

    private List<Sessao> listarAguardandoAvaliacao() {
        return sessaoRepository.findByStatus(SessaoStatus.AGUARDANDO_AVALIACAO);
    }

    private EscopoRemarcacao parseEscopo(String escopoRaw) {
        try {
            return EscopoRemarcacao.fromNullable(escopoRaw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("escopo invalido: " + escopoRaw);
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

    private void validarJanelaAtendimento(Instant dataHora) {
        LocalTime horario = dataHora.atZone(ZoneId.systemDefault()).toLocalTime();
        if (horario.isBefore(INICIO_ATENDIMENTO) || !horario.isBefore(FIM_ATENDIMENTO)) {
            throw new BusinessException("Horario fora da janela de atendimento");
        }
    }

    private void validarConflitosAgenda(Instant dataHora, String sessaoAtualId, Set<String> idsDaMesmaOperacao) {
        List<Sessao> conflitos = sessaoRepository.findByDataHoraAndStatusIn(dataHora, STATUS_CONFLITO).stream()
                .filter(s -> !s.getId().equals(sessaoAtualId))
                .filter(s -> !idsDaMesmaOperacao.contains(s.getId()))
                .toList();

        if (conflitos.isEmpty()) {
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

    // Busca sessão por ID
    private Sessao getById(String id) {
        return sessaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada: " + id));
    }

    public record RemarcacaoResultado(
            int sessoesAfetadas,
            String serieId,
            String escopoAplicado
    ) {
    }
}
