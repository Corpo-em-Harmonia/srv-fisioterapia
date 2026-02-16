package com.thalia.fisioterapia.web.controller;


import com.thalia.fisioterapia.application.service.SessaoService;
import com.thalia.fisioterapia.domain.sessao.Sessao;

import com.thalia.fisioterapia.infra.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infra.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.web.dto.sessao.RemarcarSessaoRequest;
import com.thalia.fisioterapia.web.dto.sessao.SessaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    // GET /api/sessoes?date=2026-02-14
    @GetMapping
    public ResponseEntity<List<SessaoResponse>> listarPorDia(@RequestParam("date") LocalDate date) {
        List<SessaoResponse> resp = sessaoService.listarPorDia(date).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(resp);
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
    public ResponseEntity<SessaoResponse> remarcar(@PathVariable String id, @Valid @RequestBody RemarcarSessaoRequest req) {
        return ResponseEntity.ok(toResponse(sessaoService.remarcar(id, req.dataHora())));
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
                s.getPacienteId(),
                nome,
                telefone,
                s.getDataHora().toString(),
                s.getStatus().name().toLowerCase(),
                s.getTipo().name().toLowerCase()
        );
    }
}
