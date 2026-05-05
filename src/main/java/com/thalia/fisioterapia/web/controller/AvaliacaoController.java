package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoDetalheResponse;
import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoHistoricoResponse;
import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoPendenteResponse;
import com.thalia.fisioterapia.web.dto.avaliacao.FinalizarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.avaliacao.IniciarAvaliacaoRequest;
import com.thalia.fisioterapia.application.service.AvaliacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping("/iniciar")
    public ResponseEntity<Void> iniciar(@RequestBody IniciarAvaliacaoRequest request) {
        avaliacaoService.iniciar(request.getLeadId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/finalizar")
    public ResponseEntity<Void> finalizar(@RequestBody FinalizarAvaliacaoRequest request) {
        avaliacaoService.finalizar(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<AvaliacaoPendenteResponse>> pendentes() {
        return ResponseEntity.ok(avaliacaoService.listarPendentes());
    }

    @GetMapping("/historico")
    public ResponseEntity<List<AvaliacaoHistoricoResponse>> historico() {
        return ResponseEntity.ok(avaliacaoService.listarHistorico());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoDetalheResponse> detalhe(@PathVariable String id) {
        return ResponseEntity.ok(avaliacaoService.getDetalhe(id));
    }
}
