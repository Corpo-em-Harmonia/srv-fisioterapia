package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.SessaoService;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
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
        this.pacienteRepository = pacienteRepository;
        this.leadRepository = leadRepository;
        this.sessaoService = sessaoService;
    }

    // ✅ GET /api/sessoes?periodo=pendentes&status=marcada
    @GetMapping
    public ResponseEntity<List<SessaoResponse>> listar(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) List<String> status
    ) {
        List<SessaoStatus> statusFiltro = null;
        if (status != null && !status.isEmpty()) {
            statusFiltro = status.stream()
                    .map(s -> SessaoStatus.valueOf(s.toUpperCase()))
                    .toList();
        }

        List<Sessao> sessoes;

        if (date != null) {
            // Busca por data específica
            sessoes = sessaoService.listarPorDia(date);
        } else if (periodo != null) {
            // Busca por período (pendentes, hoje, semana, mes)
            sessoes = sessaoService.listarPorPeriodo(periodo, statusFiltro);
        } else {
            // Padrão: pendentes
            sessoes = sessaoService.listarPendentes();
        }

        List<SessaoResponse> resp = sessoes.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(resp);
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

    //  NOVO: Recepção marca comparecimento de avaliação
    @PatchMapping("/{id}/compareceu-avaliacao")
    public ResponseEntity<SessaoResponse> compareceuAvaliacao(@PathVariable String id) {
        return ResponseEntity.ok(toResponse(sessaoService.marcarCompareceuAvaliacao(id)));
    }

    //  NOVO: Fisio marca avaliação como concluída
    @PatchMapping("/{id}/avaliar")
    public ResponseEntity<SessaoResponse> marcarAvaliada(@PathVariable String id) {
        return ResponseEntity.ok(toResponse(sessaoService.marcarAvaliada(id)));
    }

    // Converte lead em paciente e cria documento de avaliação
    @PostMapping("/{id}/converter-lead")
    public ResponseEntity<IniciarAvaliacaoResponse> converterLead(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.converterLeadParaPaciente(id));
    }

    // Registra evolução de uma sessão de tratamento
    @PatchMapping("/{id}/evolucao")
    public ResponseEntity<SessaoResponse> registrarEvolucao(
            @PathVariable String id,
            @RequestBody RegistrarEvolucaoRequest req
    ) {
        return ResponseEntity.ok(toResponse(sessaoService.registrarEvolucao(id, req)));
    }

    // Histórico de sessões de um paciente
    @GetMapping("/historico/{pacienteId}")
    public ResponseEntity<List<SessaoHistoricoResponse>> historico(@PathVariable String pacienteId) {
        return ResponseEntity.ok(sessaoService.getHistoricoPaciente(pacienteId));
    }

    //  NOVO: Listar aguardando avaliação (para fisio)
//    @GetMapping("/aguardando-avaliacao")
//    public ResponseEntity<List<SessaoResponse>> aguardandoAvaliacao() {
//        List<SessaoResponse> resp = sessaoService.listarAguardandoAvaliacao().stream()
//                .map(this::toResponse)
//                .toList();
//        return ResponseEntity.ok(resp);
//    }

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
            s.getNumeroOcorrencia()
        );
    }
}