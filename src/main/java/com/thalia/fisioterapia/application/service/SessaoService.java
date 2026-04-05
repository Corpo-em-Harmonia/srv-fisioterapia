package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infra.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infra.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infra.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.web.dto.sessao.SessaoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final LeadRepository leadRepository;
    private final PacienteRepository pacienteRepository;

    @Value("${app.timezone:America/Sao_Paulo}")
    private String timezone;

    public SessaoService(SessaoRepository sessaoRepository,
                         LeadRepository leadRepository,
                         PacienteRepository pacienteRepository) {
        this.sessaoRepository = sessaoRepository;
        this.leadRepository = leadRepository;
        this.pacienteRepository = pacienteRepository;
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
        ZoneId zone = ZoneId.of(timezone);
        Instant start = dia.atStartOfDay(zone).toInstant();
        Instant end = dia.plusDays(1).atStartOfDay(zone).toInstant();
        return sessaoRepository.findByDataHoraBetweenOrderByDataHoraAsc(start, end);
    }

    public List<Sessao> listarPendentes() {
        return sessaoRepository.findPendentes(Instant.now());
    }

    public List<Sessao> listarPorPeriodo(String periodo, List<SessaoStatus> statusFiltro) {
        LocalDate hoje = LocalDate.now();
        ZoneId zone = ZoneId.of(timezone);
        Instant start, end;

        switch (periodo.toLowerCase()) {
            case "hoje" -> {
                start = hoje.atStartOfDay(zone).toInstant();
                end = hoje.plusDays(1).atStartOfDay(zone).toInstant();
            }
            case "semana" -> {
                start = hoje.with(DayOfWeek.MONDAY).atStartOfDay(zone).toInstant();
                end = hoje.with(DayOfWeek.SUNDAY).plusDays(1).atStartOfDay(zone).toInstant();
            }
            case "mes" -> {
                start = hoje.withDayOfMonth(1).atStartOfDay(zone).toInstant();
                end = hoje.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant();
            }
            case "pendentes" -> {
                return listarPendentes();
            }
            case "todos" -> {
                start = Instant.EPOCH;
                end = hoje.plusYears(100).atStartOfDay(zone).toInstant();
            }
            default -> throw new IllegalArgumentException("Período inválido: " + periodo);
        }

        if (statusFiltro != null && !statusFiltro.isEmpty()) {
            return sessaoRepository.findByDataHoraBetweenAndStatusInOrderByDataHoraAsc(start, end, statusFiltro);
        }
        return sessaoRepository.findByDataHoraBetweenOrderByDataHoraAsc(start, end);
    }

    public Map<String, Object> obterEstatisticas() {
        LocalDate hoje = LocalDate.now();
        ZoneId zone = ZoneId.of(timezone);
        Instant inicioHoje = hoje.atStartOfDay(zone).toInstant();
        Instant fimHoje = hoje.plusDays(1).atStartOfDay(zone).toInstant();

        long hojeTotal = sessaoRepository.countByDataHoraBetween(inicioHoje, fimHoje);
        long pendentes = sessaoRepository.findPendentes(Instant.now()).size();
        long compareceu = sessaoRepository.countByStatus(SessaoStatus.COMPARECEU);
        long faltou = sessaoRepository.countByStatus(SessaoStatus.FALTOU);
        long total = sessaoRepository.count();

        long pessoasComFaltas = sessaoRepository.findByStatus(SessaoStatus.FALTOU)
                .stream()
                .map(s -> s.getPacienteId() != null ? s.getPacienteId() : s.getLeadId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long pessoasQueCompareceram = sessaoRepository.findByStatus(SessaoStatus.COMPARECEU)
                .stream()
                .map(s -> s.getPacienteId() != null ? s.getPacienteId() : s.getLeadId())
                .filter(Objects::nonNull)
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
        s.cancelar();
        return sessaoRepository.save(s);
    }

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

    public SessaoResponse toResponse(Sessao s) {
        String nome = null;
        String telefone = null;

        if (s.getPacienteId() != null) {
            var p = pacienteRepository.findById(s.getPacienteId()).orElse(null);
            if (p != null) { nome = p.getNome(); telefone = p.getTelefone(); }
        } else if (s.getLeadId() != null) {
            var l = leadRepository.findById(s.getLeadId()).orElse(null);
            if (l != null) { nome = l.getNome(); telefone = l.getTelefone(); }
        }

        return new SessaoResponse(
                s.getId(),
                s.getPacienteId(),
                nome,
                telefone,
                s.getDataHora().toString(),
                s.getStatus().name().toLowerCase(),
                s.getTipo().name().toLowerCase()
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

    private Sessao getById(String id) {
        return sessaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada: " + id));
    }
}