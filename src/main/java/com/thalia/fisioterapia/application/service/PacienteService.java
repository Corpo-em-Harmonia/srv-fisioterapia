package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.domain.sessao.DiaSemanaPreferido;
import com.thalia.fisioterapia.domain.sessao.ModoAgendamento;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.avaliacao.AvaliacaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import com.thalia.fisioterapia.web.dto.paciente.PacienteAtivoResponse;
import com.thalia.fisioterapia.web.dto.sessao.AgendarSessoesRequest;
import com.thalia.fisioterapia.web.dto.sessao.AgendarSessoesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final SessaoRepository sessaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public PacienteService(
            PacienteRepository pacienteRepository,
            SessaoRepository sessaoRepository,
            AvaliacaoRepository avaliacaoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.pacienteRepository = pacienteRepository;
        this.sessaoRepository = sessaoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Page<PacienteAtivoResponse> listarAtivos(Pageable pageable, boolean meusPacientes) {
        Instant agora = Instant.now();

        Page<Paciente> pagina;
        if (meusPacientes) {
            String fisioterapeutaId = fisioterapeutaIdAutenticado();
            pagina = fisioterapeutaId != null
                    ? pacienteRepository.findByFisioterapeutaId(fisioterapeutaId, pageable)
                    : pacienteRepository.findAll(pageable);
        } else {
            pagina = pacienteRepository.findAll(pageable);
        }

        // Busca em lote (findByPacienteIdIn / findByPacienteIdInOrder...) em vez de 3 consultas
        // por paciente — evita repetir o N+1 que deixava /api/sessoes lento com paginas grandes.
        var pacienteIds = pagina.getContent().stream().map(Paciente::getId).toList();

        Map<String, List<Sessao>> sessoesPorPaciente = sessaoRepository
                .findByPacienteIdInOrderByDataHoraAsc(pacienteIds).stream()
                .collect(Collectors.groupingBy(Sessao::getPacienteId));

        Map<String, Avaliacao> ultimaAvaliacaoPorPaciente = avaliacaoRepository
                .findByPacienteIdInOrderByCriadaEmDesc(pacienteIds).stream()
                .collect(Collectors.toMap(Avaliacao::getPacienteId, av -> av, (primeira, outra) -> primeira));

        var fisioterapeutaIds = pagina.getContent().stream()
                .map(Paciente::getFisioterapeutaId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, Usuario> fisiosPorId = new HashMap<>();
        usuarioRepository.findAllById(fisioterapeutaIds).forEach(u -> fisiosPorId.put(u.getId(), u));

        List<PacienteAtivoResponse> content = pagina.getContent().stream()
                .map(paciente -> {
                    List<Sessao> sessoes = sessoesPorPaciente.getOrDefault(paciente.getId(), List.of());

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

                    Avaliacao ultimaAvaliacao = ultimaAvaliacaoPorPaciente.get(paciente.getId());
                    String statusClinico = ultimaAvaliacao != null && ultimaAvaliacao.getStatus() != null
                            ? ultimaAvaliacao.getStatus().name().toLowerCase()
                            : "sem_avaliacao";

                    long sessoesRealizadas = sessoes.stream()
                            .filter(s -> s.getStatus() == SessaoStatus.REALIZADA
                                    || s.getStatus() == SessaoStatus.COMPARECEU
                                    || s.getStatus() == SessaoStatus.AVALIADA)
                            .count();

                    String fisioterapeutaNome = paciente.getFisioterapeutaId() != null
                            ? Optional.ofNullable(fisiosPorId.get(paciente.getFisioterapeutaId()))
                                    .map(Usuario::getNome).orElse(null)
                            : null;

                    return new PacienteAtivoResponse(
                            paciente.getId(),
                            nomeCompleto(paciente),
                            ultimaSessao != null ? ultimaSessao.toString() : null,
                            proximaSessao != null ? proximaSessao.toString() : null,
                            sessoes.size(),
                            sessoesRealizadas,
                            statusClinico,
                            paciente.getFisioterapeutaId(),
                            fisioterapeutaNome
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, pagina.getTotalElements());
    }

    @Transactional
    public void reatribuirFisioterapeuta(String pacienteId, String fisioterapeutaId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        if (fisioterapeutaId != null && !fisioterapeutaId.isBlank()) {
            usuarioRepository.findById(fisioterapeutaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fisioterapeuta não encontrada"));
        }

        paciente.atribuirFisioterapeuta(fisioterapeutaId);
        pacienteRepository.save(paciente);

        // Sessões futuras (ainda não realizadas) passam a ser da nova fisio;
        // sessões já concluídas mantêm o histórico de quem realmente atendeu.
        List<Sessao> sessoesPendentes = sessaoRepository.findByPacienteIdOrderByDataHoraAsc(pacienteId).stream()
                .filter(s -> AgendaUtil.STATUS_CONFLITO.contains(s.getStatus()))
                .toList();

        for (Sessao sessao : sessoesPendentes) {
            sessao.atribuirFisioterapeuta(fisioterapeutaId);
        }
        sessaoRepository.saveAll(sessoesPendentes);
    }

    private String fisioterapeutaIdAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return usuarioRepository.findByEmail(auth.getName())
                .map(Usuario::getId)
                .orElse(null);
    }

    @Transactional
    public AgendarSessoesResponse agendarSessoes(String pacienteId, AgendarSessoesRequest req) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        ModoAgendamento modo = parseModo(req.modoAgendamento());
        LocalDateTime primeiraDataHora = req.dataHora();
        AgendaUtil.validarJanela(primeiraDataHora);

        int frequencia = 1;
        int quantidade = 1;
        List<LocalDateTime> datas;

        if (modo == ModoAgendamento.RECORRENTE) {
            // Antes usava defaults silenciosos (frequência=1, quantidade=9) quando o cliente
            // esquecia esses campos — inconsistente com LeadService.agendarAvaliacao, que rejeita
            // a mesma omissão com 400. Um typo no front podia criar 9 sessões semanais sem avisar.
            if (req.frequenciaSemanal() == null) {
                throw new BusinessException("frequenciaSemanal obrigatorio para modo recorrente");
            }
            if (req.quantidadeSessoes() == null) {
                throw new BusinessException("quantidadeSessoes obrigatorio para modo recorrente");
            }
            frequencia = req.frequenciaSemanal();
            quantidade = req.quantidadeSessoes();
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
            validarConflito(instant, paciente.getFisioterapeutaId());

            Sessao sessao = new Sessao(pacienteId, req.avaliacaoId(), instant, req.observacao());
            if (serieId != null) sessao.definirSerie(serieId, i + 1);
            if (paciente.getFisioterapeutaId() != null) {
                sessao.atribuirFisioterapeuta(paciente.getFisioterapeutaId());
            }
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

    private void validarConflito(Instant dataHora, String fisioterapeutaId) {
        List<Sessao> conflitos = sessaoRepository.findByDataHoraAndStatusIn(dataHora, AgendaUtil.STATUS_CONFLITO);

        if (fisioterapeutaId != null && !fisioterapeutaId.isBlank()) {
            boolean fisioOcupada = conflitos.stream()
                    .anyMatch(s -> fisioterapeutaId.equals(s.getFisioterapeutaId()));
            if (fisioOcupada) {
                throw new AgendaConflictException("Já existe sessão nesse horário para esta fisioterapeuta", List.of());
            }
        }

        if (conflitos.size() >= AgendaUtil.MAX_POR_HORARIO) {
            var pacienteIds = conflitos.stream().map(Sessao::getPacienteId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<String, Paciente> pacientesPorId = new HashMap<>();
            pacienteRepository.findAllById(pacienteIds).forEach(p -> pacientesPorId.put(p.getId(), p));

            List<AgendaConflictException.ConflitoAgendaItem> itens = conflitos.stream()
                    .map(s -> new AgendaConflictException.ConflitoAgendaItem(
                            s.getId(),
                            s.getDataHora(),
                            Optional.ofNullable(pacientesPorId.get(s.getPacienteId()))
                                    .map(Paciente::getNome)
                                    .orElse(s.getPacienteId() != null ? "Paciente" : "Lead")
                    ))
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
