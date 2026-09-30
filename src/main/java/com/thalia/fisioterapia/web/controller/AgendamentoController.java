package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.SessaoService;
import com.thalia.fisioterapia.web.dto.sessao.DisponibilidadeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final SessaoService sessaoService;

    public AgendamentoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<List<DisponibilidadeResponse>> disponibilidade(
            @RequestParam("date") LocalDate date,
            @RequestParam(value = "excludeId", required = false) String excludeId,
            @RequestParam(value = "fisioterapeutaId", required = false) String fisioterapeutaId) {
        return ResponseEntity.ok(sessaoService.consultarDisponibilidade(date, excludeId, fisioterapeutaId));
    }
}
