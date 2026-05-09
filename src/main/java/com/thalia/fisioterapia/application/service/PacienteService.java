package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.PlanoForaValidadeException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.avaliacao.AvaliacaoStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PacienteService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalTime INICIO_ATENDIMENTO = LocalTime.of(8, 0);
    private static final LocalTime FIM_ATENDIMENTO = LocalTime.of(20, 0);
    private static final int VALIDADE_GUIA_PADRAO_DIAS = 30;
    private static final List<SessaoStatus> STATUS_CONFLITO = List.of(
            SessaoStatus.MARCADA, SessaoStatus.REMARCADA, SessaoStatus.AGUARDANDO_AVALIACAO
    );

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
                            .map(Sessao::getDataHora)
                            .filter(data -> !data.isAfter(agora))
                            .reduce((first, second) -> second)
                            .orElse(null);

                    Instant proximaSessao = sessoes.stream()
                            .map(Sessao::getDataHora)
                            .filter(data -> data.isAfter(agora))
                            .findFirst()
                            .orElse(null);

                    String statusClinico = avaliacaoRepository.findFirstByPacienteIdOrderByCriadaEmDesc(paciente.getId())
                            .map(avaliacao -> {
                                AvaliacaoStatus status = avaliacao.getStatus();
                                return status != null ? status.name().toLowerCase() : "sem_avaliacao";
                            })
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
    }

    @Transactional
    public AgendarSessoesResponse agendarSessoes(String pacienteId, AgendarSessoesRequest req) {
        pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        ModoAgendamento modo = parseModo(req.modoAgendamento());
        LocalDateTime primeiraDataHora = req.dataHora();
        validarJanela(primeiraDataHora);

        int frequencia = 1;
        int quantidade = 1;
        List<LocalDateTime> datas;

        if (modo == ModoAgendamento.RECORRENTE) {
            frequencia = req.frequenciaSemanal() != null ? req.frequenciaSemanal() : 1;
            quantidade = req.quantidadeSessoes() != null ? req.quantidadeSessoes() : 9;
            int validade = req.validadeGuiaDias() != null ? req.validadeGuiaDias() : VALIDADE_GUIA_PADRAO_DIAS;
            validarPlano(quantidade, frequencia, validade);
            Set<DayOfWeek> dias = parseDias(req.diasSemanaPreferidos());
            datas = gerarDatas(primeiraDataHora, quantidade, frequencia, dias);
        } else {
            datas = List.of(primeiraDataHora);
        }

        String serieId = modo == ModoAgendamento.RECORRENTE
                ? "sr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                : null;

        List<Sessao> sessoesParaSalvar = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            LocalDateTime dt = datas.get(i);
            validarJanela(dt);
            Instant instant = dt.atZone(DEFAULT_ZONE).toInstant();
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

    private void validarJanela(LocalDateTime dataHora) {
        DayOfWeek dia = dataHora.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new BusinessException("Não é permitido agendar sessões nos fins de semana");
        }
        LocalTime horario = dataHora.toLocalTime();
        if (horario.isBefore(INICIO_ATENDIMENTO) || !horario.isBefore(FIM_ATENDIMENTO)) {
            throw new BusinessException("Horário fora da janela de atendimento (08h-20h)");
        }
    }

    private static final int MAX_POR_HORARIO = 6;

    private void validarConflito(Instant dataHora) {
        List<Sessao> conflitos = sessaoRepository.findByDataHoraAndStatusIn(dataHora, STATUS_CONFLITO);
        if (conflitos.size() >= MAX_POR_HORARIO) {
            List<AgendaConflictException.ConflitoAgendaItem> itens = conflitos.stream()
                    .map(s -> new AgendaConflictException.ConflitoAgendaItem(s.getId(), s.getDataHora(), ""))
                    .toList();
            throw new AgendaConflictException("Já existe sessão nesse horário", itens);
        }
    }

    private void validarPlano(int quantidade, int frequencia, int validade) {
        int semanas = (quantidade + frequencia - 1) / frequencia;
        int duracao = semanas * 7;
        if (duracao > validade) {
            int frequenciaMinima = 1;
            for (int f = 1; f <= 7; f++) {
                if (((quantidade + f - 1) / f) * 7 <= validade) { frequenciaMinima = f; break; }
            }
            throw new PlanoForaValidadeException(
                    "Plano não cabe na validade da guia", duracao, validade, frequenciaMinima);
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

    private List<LocalDateTime> gerarDatas(LocalDateTime primeira, int quantidade, int frequencia, Set<DayOfWeek> diasPreferidos) {
        List<LocalDateTime> resultado = new ArrayList<>();
        LocalTime horario = primeira.toLocalTime();
        LocalDate inicio = primeira.toLocalDate();
        LocalDate cursor = inicio;

        while (resultado.size() < quantidade) {
            LocalDate fim = cursor.plusDays(6);
            List<LocalDate> preferidos = new ArrayList<>();
            List<LocalDate> fallback = new ArrayList<>();

            for (LocalDate dia = cursor; !dia.isAfter(fim); dia = dia.plusDays(1)) {
                if (dia.isBefore(inicio)) continue;
                if (!diasPreferidos.isEmpty() && diasPreferidos.contains(dia.getDayOfWeek())) {
                    preferidos.add(dia);
                } else if (diasPreferidos.isEmpty()) {
                    fallback.add(dia);
                } else {
                    fallback.add(dia);
                }
            }

            preferidos.sort(Comparator.naturalOrder());
            fallback.sort(Comparator.naturalOrder());

            int restantes = frequencia;
            for (LocalDate data : preferidos) {
                if (restantes == 0 || resultado.size() == quantidade) break;
                resultado.add(LocalDateTime.of(data, horario));
                restantes--;
            }
            for (LocalDate data : fallback) {
                if (restantes == 0 || resultado.size() == quantidade) break;
                resultado.add(LocalDateTime.of(data, horario));
                restantes--;
            }
            cursor = cursor.plusDays(7);
        }
        return resultado;
    }

    private String nomeCompleto(Paciente paciente) {
        String sobrenome = paciente.getSobrenome();
        if (sobrenome == null || sobrenome.isBlank()) return paciente.getNome();
        return (paciente.getNome() + " " + sobrenome).trim();
    }
}
