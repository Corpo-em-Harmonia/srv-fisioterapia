package com.thalia.fisioterapia.web.controllers;

import com.thalia.fisioterapia.application.services.AgendaService;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.web.dto.AgendarAvaliacaoRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leads")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @PostMapping("/{leadId}/agendar-avaliacao")
    public ResponseEntity<Sessao> agendar(@PathVariable String leadId,
                                          @RequestBody AgendarAvaliacaoRequest request) {
        Sessao sessao = agendaService.agendarAvaliacao(leadId, request.dataHora());
        return ResponseEntity.ok(sessao);
    }
}
