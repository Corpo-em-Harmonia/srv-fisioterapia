package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.AgendaService;
import com.thalia.fisioterapia.web.dto.sessao.DisponibilidadeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendaService agendaService;

    public AgendamentoController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<List<DisponibilidadeResponse>> disponibilidade(@RequestParam("date") LocalDate date) {
        return ResponseEntity.ok(agendaService.obterDisponibilidade(date));
    }
}



