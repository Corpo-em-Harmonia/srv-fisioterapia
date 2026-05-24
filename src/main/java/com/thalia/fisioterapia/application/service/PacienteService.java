package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.domain.sessao.DiaSemanaPreferido;
import com.thalia.fisioterapia.domain.sessao.ModoAgendamento;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infrastructure.repository.avaliacao.AvaliacaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.web.dto.paciente.PacienteAtivoResponse;
import com.thalia.fisioterapia.web.dto.sessao.AgendarSessoesRequest;
import com.thalia.fisioterapia.web.dto.sessao.AgendarSessoesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    public Page<PacienteAtivoResponse> listarAtivos(Pageable pageable) {
        Instant agora = Instant.now();
        Page<Paciente> pagina = pacienteRepository.findAll(pageable);

        List<PacienteAtivoResponse> content = pagina.getContent().stream()
                .map(paciente -> {
                    var sessoes = sessaoRepository.findByPacienteIdOrderByDataHoraAsc(paciente.getId());

                    Instant ultimaSessao = sessoes.stream()
                            .map(Sessao::getDataHora)
                            .filter(data -> !data.isAfter(agora))
                            .reduce((first, second) -> second)
                            .orElse(null);

                    Instant proximaSessao = sessoes.stream()
                            .map(Sessao::getDataHora)
                            .filter(data -> data.isAfter(agora))
                            .findFirst()
                            .orElse(null);

                    String statusClinico = avaliacaoRepository
                            .findFirstByPacienteIdOrderByCriadaEmDesc(paciente.getId())
                            .map(av -> av.getStatus() != null
                                    ? av.getStatus().name().toLowerCase() : "sem_avaliacao")
                            .orElse("sem_avaliacao");

                    long sessoesRealizadas = sessoes.stream()
                            .filter(s -> s.getStatus() == SessaoStatus.REALIZADA
                                    || s.getStatus() == SessaoStatus.COMPARECEU
                                    || s.getStatus() == SessaoStatus.AVALIADA)
                            .count();

                    return new PacienteAtivoResponse(
                            paciente.getId(),
                            nomeCompleto(paciente),
                            ultimaSessao != null ? ultimaSessao.toString() : null,
                            proximaSessao != null ? proximaSessao.toString() : null,
                            sessoes.size(),
                            sessoesRealizadas,
                            statusClinico
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, pagina.getTotalElements());
    }

    @Transactional
    public AgendarSessoesResponse agendarSessoes(String pacienteId, AgendarSessoesRequest req) {
        pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        ModoAgendamento modo = parseModo(req.modoAgendamento());
        LocalDateTime primeiraDataHora = req.dataHora();
        AgendaUtil.validarJanela(primeiraDataHora);

        int frequencia = 1;
        int quantidade = 1;
        List<LocalDateTime> datas;

        if (modo == ModoAgendamento.RECORRENTE) {
            frequencia = req.frequenciaSemanal() != null ? req.frequenciaSemanal() : 1;
            quantidade = req.quantidadeSessoes() != null ? req.quantidadeSessoes() : 9;
            int validade = req.validadeGuiaDias() != null ? req.validadeGuiaDias() : AgendaUtil.VALIDADE_GUIA_PADRAO_DIAS;
            AgendaUtil.validarPlano(quantidade, frequencia, validade);
            Set<DayOfWeek> dias = parseDias(req.diasSemanaPreferidos());
            datas = AgendaUtil.gerarDatas(primeiraDataHora, quantidade, frequencia, dias);
        } else {
            datas = List.of(primeiraDataHora);
        }

        String serieId = modo == ModoAgendamento.RECORRENTE
                ? "sr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                : null;

        List<Sessao> sessoesParaSalvar = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            LocalDateTime dt = datas.get(i);
            AgendaUtil.validarJanela(dt);
            Instant instant = dt.atZone(AgendaUtil.ZONE_SP).toInstant();
            validarConflito(instant);

            Sessao sessao = new Sessao(pacienteId, req.avaliacaoId(), instant, req.observacao());
            if (serieId != null) sessao.definirSerie(serieId, i + 1);
            sessoesParaSalvar.add(sessao);
        }

        List<Sessao> salvas = sessaoRepository.saveAll(sessoesParaSalvar);

        return new AgendarSessoesResponse(
                modo.name().toLowerCase(),
                serieId,
                salvas.size(),
                "Sessões agendadas com sucesso"
        );
    }

    private void validarConflito(Instant dataHora) {
        List<Sessao> conflitos = sessaoRepository.findByDataHoraAndStatusIn(dataHora, AgendaUtil.STATUS_CONFLITO);
        if (conflitos.size() >= AgendaUtil.MAX_POR_HORARIO) {
            List<AgendaConflictException.ConflitoAgendaItem> itens = conflitos.stream()
                    .map(s -> new AgendaConflictException.ConflitoAgendaItem(s.getId(), s.getDataHora(), ""))
                    .toList();
            throw new AgendaConflictException("Já existe sessão nesse horário", itens);
        }
    }

    private ModoAgendamento parseModo(String modo) {
        try {
            return ModoAgendamento.fromNullable(modo);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("modoAgendamento inválido: %s".formatted(modo));
        }
    }

    private Set<DayOfWeek> parseDias(List<String> dias) {
        if (dias == null || dias.isEmpty()) return Set.of();
        Set<DayOfWeek> resultado = new HashSet<>();
        for (String dia : dias) {
            try {
                resultado.add(DiaSemanaPreferido.fromCode(dia).getDayOfWeek());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ex.getMessage());
            }
        }
        return resultado;
    }

    private String nomeCompleto(Paciente paciente) {
        String sobrenome = paciente.getSobrenome();
        if (sobrenome == null || sobrenome.isBlank()) return paciente.getNome();
        return (paciente.getNome() + " " + sobrenome).trim();
    }
}
