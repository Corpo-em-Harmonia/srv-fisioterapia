package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.SessaoService;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.web.dto.sessao.RemarcarSessaoRequest;
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

    public SessaoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
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
                    .map(s -> SessaoStatus.valueOf(s.toUpperCase()))
                    .toList();
        }

        var sessoes = date != null
                ? sessaoService.listarPorDia(date)
                : periodo != null
                ? sessaoService.listarPorPeriodo(periodo, statusFiltro)
                : sessaoService.listarPendentes();

        return ResponseEntity.ok(sessoes.stream().map(sessaoService::toResponse).toList());
    }

    @GetMapping("/estatisticas")
    public ResponseEntity<Map<String, Object>> estatisticas() {
        return ResponseEntity.ok(sessaoService.obterEstatisticas());
    }

    @PatchMapping("/{id}/compareceu")
    public ResponseEntity<SessaoResponse> compareceu(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.toResponse(sessaoService.marcarCompareceu(id)));
    }

    @PatchMapping("/{id}/faltou")
    public ResponseEntity<SessaoResponse> faltou(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.toResponse(sessaoService.marcarFaltou(id)));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SessaoResponse> cancelar(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.toResponse(sessaoService.cancelar(id)));
    }

    @PatchMapping("/{id}/remarcar")
    public ResponseEntity<SessaoResponse> remarcar(@PathVariable String id, @Valid @RequestBody RemarcarSessaoRequest req) {
        return ResponseEntity.ok(sessaoService.toResponse(sessaoService.remarcar(id, req.dataHora())));
    }

    @PatchMapping("/{id}/compareceu-avaliacao")
    public ResponseEntity<SessaoResponse> compareceuAvaliacao(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.toResponse(sessaoService.marcarCompareceuAvaliacao(id)));
    }

    @PatchMapping("/{id}/avaliar")
    public ResponseEntity<SessaoResponse> marcarAvaliada(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.toResponse(sessaoService.marcarAvaliada(id)));
    }
}