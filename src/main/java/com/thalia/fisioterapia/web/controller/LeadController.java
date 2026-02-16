package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.LeadService;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.lead.LeadStatus;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.web.dto.agenda.AgendarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.lead.CriarLeadRequest;
import com.thalia.fisioterapia.web.dto.lead.CriarLeadResponse;
import com.thalia.fisioterapia.web.dto.lead.ExecutarAcaoLeadRequest;
import com.thalia.fisioterapia.web.dto.lead.LeadResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public ResponseEntity<CriarLeadResponse> criaLead(@Valid @RequestBody CriarLeadRequest request) {
        Lead lead = leadService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CriarLeadResponse(
                lead.getId(),
                lead.getNome(),
                lead.getTelefone(),
                lead.getStatus().name()
        ));
    }

    @GetMapping
    public ResponseEntity<List<LeadResponse>> listar(@RequestParam(value = "ativos", required = false) Boolean ativos) {
        if (Boolean.TRUE.equals(ativos)) {
            return ResponseEntity.ok(leadService.listarAtivos());
        }
        return ResponseEntity.ok(leadService.listarTodos());
    }

    // ✅ Ações simples (sem modal)
    @PostMapping("/{id}/acoes")
    public ResponseEntity<Lead> executarAcao(
            @PathVariable String id,
            @Valid @RequestBody ExecutarAcaoLeadRequest request
    ) {
        Lead updated = leadService.executarAcaoSimples(id, request.acao());
        return ResponseEntity.ok(updated);
    }

    // ✅ Agendar avaliação (com payload) → cria Sessao + muda Lead p/ AGENDADO
    @PostMapping("/{id}/agendar-avaliacao")
    public ResponseEntity<Sessao> agendarAvaliacao(
            @PathVariable String id,
            @Valid @RequestBody AgendarAvaliacaoRequest request
    ) {
        Sessao sessao = leadService.agendarAvaliacao(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sessao);
    }
}
