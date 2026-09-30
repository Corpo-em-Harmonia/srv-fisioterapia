package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.SessaoService;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import com.thalia.fisioterapia.web.dto.avaliacao.IniciarAvaliacaoResponse;
import com.thalia.fisioterapia.web.dto.sessao.RegistrarEvolucaoRequest;
import com.thalia.fisioterapia.web.dto.sessao.RemarcarSessaoRequest;
import com.thalia.fisioterapia.web.dto.sessao.RemarcarSessaoResponse;
import com.thalia.fisioterapia.web.dto.sessao.SessaoHistoricoResponse;
import com.thalia.fisioterapia.web.dto.sessao.SessaoResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessoes")
public class SessaoController {

    private final SessaoService sessaoService;
    private final PacienteRepository pacienteRepository;
    private final LeadRepository leadRepository;
    private final UsuarioRepository usuarioRepository;

    public SessaoController(SessaoService sessaoService, PacienteRepository pacienteRepository,
                             LeadRepository leadRepository, UsuarioRepository usuarioRepository) {
        this.sessaoService = sessaoService;
        this.pacienteRepository = pacienteRepository;
        this.leadRepository = leadRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<Page<SessaoResponse>> listar(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        List<SessaoStatus> statusFiltro = null;
        if (status != null && !status.isEmpty()) {
            statusFiltro = status.stream()
                    .map(s -> {
                        try {
                            return SessaoStatus.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new com.thalia.fisioterapia.application.exception.BusinessException(
                                    "Status inválido: %s".formatted(s));
                        }
                    })
                    .toList();
        }

        var pageable = PageRequest.of(page, size, Sort.by("dataHora").ascending());
        Page<Sessao> sessoes;

        if (date != null) {
            sessoes = sessaoService.listarPorDia(date, statusFiltro, pageable);
        } else if (periodo != null) {
            sessoes = sessaoService.listarPorPeriodo(periodo, statusFiltro, pageable);
        } else {
            sessoes = sessaoService.listarPendentes(statusFiltro, pageable);
        }

        return ResponseEntity.ok(toResponsePage(sessoes));
    }

    @GetMapping("/estatisticas")
    public ResponseEntity<Map<String, Object>> estatisticas() {
        return ResponseEntity.ok(sessaoService.obterEstatisticas());
    }

    @PatchMapping("/{id}/compareceu")
    public ResponseEntity<SessaoResponse> compareceu(@PathVariable String id) {
        return ResponseEntity.ok(toResponse(sessaoService.marcarCompareceu(id)));
    }

    @PatchMapping("/{id}/faltou")
    public ResponseEntity<SessaoResponse> faltou(@PathVariable String id) {
        return ResponseEntity.ok(toResponse(sessaoService.marcarFaltou(id)));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SessaoResponse> cancelar(@PathVariable String id) {
        return ResponseEntity.ok(toResponse(sessaoService.cancelar(id)));
    }

    @PatchMapping("/{id}/remarcar")
    public ResponseEntity<RemarcarSessaoResponse> remarcar(@PathVariable String id, @Valid @RequestBody RemarcarSessaoRequest req) {
        SessaoService.RemarcacaoResultado resultado = sessaoService.remarcar(id, req.dataHora(), req.escopo(), req.motivo());
        return ResponseEntity.ok(new RemarcarSessaoResponse(
                resultado.sessoesAfetadas(),
                resultado.serieId(),
                resultado.escopoAplicado()
        ));
    }

    @PatchMapping("/{id}/compareceu-avaliacao")
    public ResponseEntity<SessaoResponse> compareceuAvaliacao(@PathVariable String id) {
        return ResponseEntity.ok(toResponse(sessaoService.marcarCompareceuAvaliacao(id)));
    }

    @PatchMapping("/{id}/avaliar")
    public ResponseEntity<SessaoResponse> marcarAvaliada(@PathVariable String id) {
        return ResponseEntity.ok(toResponse(sessaoService.marcarAvaliada(id)));
    }

    @PostMapping("/{id}/converter-lead")
    public ResponseEntity<IniciarAvaliacaoResponse> converterLead(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.converterLeadParaPaciente(id));
    }

    @PatchMapping("/{id}/evolucao")
    public ResponseEntity<SessaoResponse> registrarEvolucao(
            @PathVariable String id,
            @Valid @RequestBody RegistrarEvolucaoRequest req
    ) {
        return ResponseEntity.ok(toResponse(sessaoService.registrarEvolucao(id, req)));
    }

    @GetMapping("/historico/{pacienteId}")
    public ResponseEntity<List<SessaoHistoricoResponse>> historico(@PathVariable String pacienteId) {
        return ResponseEntity.ok(sessaoService.getHistoricoPaciente(pacienteId));
    }

    /**
     * Busca paciente/lead/fisioterapeuta de cada sessão em lote (findAllById) em vez de
     * uma consulta por sessão — com size=500 isso evitava até ~1000 round-trips ao Mongo.
     */
    private Page<SessaoResponse> toResponsePage(Page<Sessao> sessoes) {
        List<Sessao> content = sessoes.getContent();

        var pacienteIds = content.stream().map(Sessao::getPacienteId).filter(Objects::nonNull).collect(Collectors.toSet());
        var leadIds = content.stream().map(Sessao::getLeadId).filter(Objects::nonNull).collect(Collectors.toSet());
        var fisioIds = content.stream().map(Sessao::getFisioterapeutaId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<String, Paciente> pacientesPorId = toMapById(pacienteRepository.findAllById(pacienteIds), Paciente::getId);
        Map<String, Lead> leadsPorId = toMapById(leadRepository.findAllById(leadIds), Lead::getId);
        Map<String, Usuario> fisiosPorId = toMapById(usuarioRepository.findAllById(fisioIds), Usuario::getId);

        return sessoes.map(s -> toResponse(s, pacientesPorId, leadsPorId, fisiosPorId));
    }

    private <T> Map<String, T> toMapById(Iterable<T> itens, java.util.function.Function<T, String> idExtractor) {
        Map<String, T> mapa = new java.util.HashMap<>();
        itens.forEach(item -> mapa.put(idExtractor.apply(item), item));
        return mapa;
    }

    private SessaoResponse toResponse(Sessao s) {
        Paciente p = s.getPacienteId() != null ? pacienteRepository.findById(s.getPacienteId()).orElse(null) : null;
        Lead l = s.getLeadId() != null ? leadRepository.findById(s.getLeadId()).orElse(null) : null;
        Usuario fisio = s.getFisioterapeutaId() != null ? usuarioRepository.findById(s.getFisioterapeutaId()).orElse(null) : null;
        return toResponse(
                s,
                p != null ? Map.of(p.getId(), p) : Map.of(),
                l != null ? Map.of(l.getId(), l) : Map.of(),
                fisio != null ? Map.of(fisio.getId(), fisio) : Map.of()
        );
    }

    private SessaoResponse toResponse(Sessao s, Map<String, Paciente> pacientesPorId,
                                       Map<String, Lead> leadsPorId, Map<String, Usuario> fisiosPorId) {
        String nome = null;
        String telefone = null;

        if (s.getPacienteId() != null) {
            Paciente p = pacientesPorId.get(s.getPacienteId());
            if (p != null) { nome = p.getNome(); telefone = p.getTelefone(); }
        } else if (s.getLeadId() != null) {
            Lead l = leadsPorId.get(s.getLeadId());
            if (l != null) { nome = l.getNome(); telefone = l.getTelefone(); }
        }

        String fisioterapeutaNome = s.getFisioterapeutaId() != null
                ? java.util.Optional.ofNullable(fisiosPorId.get(s.getFisioterapeutaId())).map(Usuario::getNome).orElse(null)
                : null;

        return new SessaoResponse(
                s.getId(),
                s.getLeadId(),
                s.getPacienteId(),
                nome,
                telefone,
                s.getDataHora().toString(),
                s.getStatus().name().toLowerCase(),
                s.getTipo().name().toLowerCase(),
                s.getSerieId(),
                s.getNumeroOcorrencia(),
                s.getEvolucao(),
                s.getFisioterapeutaId(),
                fisioterapeutaNome
        );
    }
}
