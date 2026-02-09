package com.thalia.fisioterapia.web.controllers;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.web.dto.CriarLeadRequest;
import com.thalia.fisioterapia.web.dto.CriarLeadResponse;
import com.thalia.fisioterapia.web.dto.ExecutarAcaoLeadRequest;
import com.thalia.fisioterapia.application.services.LeadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController( LeadService leadService) {

        this.leadService = leadService;
    }

    @PostMapping("/{id}/acoes")
    public ResponseEntity<Void> executarAcao(
            @PathVariable String id,
            @RequestBody ExecutarAcaoLeadRequest request
    ) {
        leadService.executarAcao(id, request.acao());
        return ResponseEntity.ok().build();
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
    public ResponseEntity<List<Lead>> listar() {
        return ResponseEntity.ok(leadService.allLead());
    }

}
