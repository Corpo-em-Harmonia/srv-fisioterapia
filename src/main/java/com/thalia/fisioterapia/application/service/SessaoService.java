package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infra.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infra.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infra.repository.sessao.SessaoRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SessaoService {

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

            default -> throw new IllegalArgumentException("Período inválido: " + periodo);
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
        s.cancelar();
        return sessaoRepository.save(s);
    }

    // Remarca sessão (verifica se horário está livre)
    public Sessao remarcar(String id, Instant novaDataHora) {
        Sessao s = getById(id);

        boolean ocupado = sessaoRepository.existsByDataHoraAndStatusIn(
                novaDataHora,
                List.of(SessaoStatus.MARCADA, SessaoStatus.REMARCADA)
        );
        if (ocupado) {
            throw new IllegalArgumentException("Horário já está ocupado.");
        }

        s.remarcar(novaDataHora);
        return sessaoRepository.save(s);
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

    // Busca sessão por ID
    private Sessao getById(String id) {
        return sessaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada: " + id));
    }
}