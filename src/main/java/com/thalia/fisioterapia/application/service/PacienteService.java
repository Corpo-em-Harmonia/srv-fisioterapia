package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.domain.avaliacao.AvaliacaoStatus;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.infrastructure.repository.avaliacao.AvaliacaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.web.dto.paciente.PacienteAtivoResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final SessaoRepository sessaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public PacienteService(
            PacienteRepository pacienteRepository,
            SessaoRepository sessaoRepository,
            AvaliacaoRepository avaliacaoRepository
    ) {
        this.pacienteRepository = pacienteRepository;
        this.sessaoRepository = sessaoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public List<PacienteAtivoResponse> listarAtivos() {
        Instant agora = Instant.now();

        return pacienteRepository.findAll().stream()
                .map(paciente -> {
                    var sessoes = sessaoRepository.findByPacienteIdOrderByDataHoraAsc(paciente.getId());

                    Instant ultimaSessao = sessoes.stream()
                            .map(s -> s.getDataHora())
                            .filter(data -> !data.isAfter(agora))
                            .reduce((first, second) -> second)
                            .orElse(null);

                    Instant proximaSessao = sessoes.stream()
                            .map(s -> s.getDataHora())
                            .filter(data -> data.isAfter(agora))
                            .findFirst()
                            .orElse(null);

                    String statusClinico = avaliacaoRepository.findFirstByPacienteIdOrderByCriadaEmDesc(paciente.getId())
                            .map(avaliacao -> {
                                AvaliacaoStatus status = avaliacao.getStatus();
                                return status != null ? status.name().toLowerCase() : "sem_avaliacao";
                            })
                            .orElse("sem_avaliacao");

                    return new PacienteAtivoResponse(
                            paciente.getId(),
                            nomeCompleto(paciente),
                            ultimaSessao != null ? ultimaSessao.toString() : null,
                            proximaSessao != null ? proximaSessao.toString() : null,
                            sessoes.size(),
                            statusClinico
                    );
                })
                .toList();
    }

    private String nomeCompleto(Paciente paciente) {
        String sobrenome = paciente.getSobrenome();
        if (sobrenome == null || sobrenome.isBlank()) {
            return paciente.getNome();
        }
        return (paciente.getNome() + " " + sobrenome).trim();
    }
}
