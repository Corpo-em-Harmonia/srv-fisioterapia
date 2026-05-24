package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.SessaoService;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.web.dto.avaliacao.IniciarAvaliacaoResponse;
import com.thalia.fisioterapia.web.dto.sessao.RegistrarEvolucaoRequest;
import com.thalia.fisioterapia.web.dto.sessao.RemarcarSessaoRequest;
import com.thalia.fisioterapia.web.dto.sessao.RemarcarSessaoResponse;
import com.thalia.fisioterapia.web.dto.sessao.SessaoHistoricoResponse;
import com.thalia.fisioterapia.web.dto.sessao.SessaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessoes")
public class SessaoController {

    private final SessaoService sessaoService;
    private final PacienteRepository pacienteRepository;
    private final LeadRepository leadRepository;

    public SessaoController(SessaoService sessaoService, PacienteRepository pacienteRepository, LeadRepository leadRepository) {
        this.sessaoService = sessaoService;
        this.pacienteRepository = pacienteRepository;
        this.leadRepository = leadRepository;
    }

    @GetMapping
    public ResponseEntity<List<SessaoResponse>> listar(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) List<String> status
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

        List<Sessao> sessoes;

        if (date != null) {
            sessoes = sessaoService.listarPorDia(date);
        } else if (periodo != null) {
            sessoes = sessaoService.listarPorPeriodo(periodo, statusFiltro);
        } else {
            sessoes = sessaoService.listarPendentes();
        }

        return ResponseEntity.ok(sessoes.stream().map(this::toResponse).toList());
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

    private SessaoResponse toResponse(Sessao s) {
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
                s.getLeadId(),
                s.getPacienteId(),
                nome,
                telefone,
                s.getDataHora().toString(),
                s.getStatus().name().toLowerCase(),
                s.getTipo().name().toLowerCase(),
                s.getSerieId(),
                s.getNumeroOcorrencia(),
                s.getEvolucao()
        );
    }
}
