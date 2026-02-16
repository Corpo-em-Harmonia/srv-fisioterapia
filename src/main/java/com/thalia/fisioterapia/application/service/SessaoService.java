package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;

import com.thalia.fisioterapia.infra.repository.sessao.SessaoRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;

    public SessaoService(SessaoRepository sessaoRepository) {
        this.sessaoRepository = sessaoRepository;
    }

    public List<Sessao> listarPorDia(LocalDate dia) {
        Instant start = dia.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = dia.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return sessaoRepository.findByDataHoraBetweenOrderByDataHoraAsc(start, end);
    }

    public Sessao marcarCompareceu(String id) {
        Sessao s = getById(id);
        s.marcarComparecimento();
        return sessaoRepository.save(s);
    }

    public Sessao marcarFaltou(String id) {
        Sessao s = getById(id);
        s.marcarFaltou();
        return sessaoRepository.save(s);
    }

    public Sessao cancelar(String id) {
        Sessao s = getById(id);
        s.cancelar();
        return sessaoRepository.save(s);
    }

    public Sessao remarcar(String id, Instant novaDataHora) {
        Sessao s = getById(id);

        // regra: não pode remarcar pra horário ocupado (MARCADA/REMARCADA)
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

    private Sessao getById(String id) {
        return sessaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada: " + id));
    }
}
