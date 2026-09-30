package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoDetalheResponse;
import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoHistoricoResponse;
import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoPendenteResponse;
import com.thalia.fisioterapia.web.dto.avaliacao.FinalizarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.avaliacao.IniciarAvaliacaoRequest;
import com.thalia.fisioterapia.application.service.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping("/iniciar")
    public ResponseEntity<Void> iniciar(@Valid @RequestBody IniciarAvaliacaoRequest request) {
        avaliacaoService.iniciar(request.getLeadId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/finalizar")
    public ResponseEntity<Void> finalizar(@Valid @RequestBody FinalizarAvaliacaoRequest request) {
        avaliacaoService.finalizar(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pendentes")
    public ResponseEntity<Page<AvaliacaoPendenteResponse>> pendentes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("dataHora").ascending());
        return ResponseEntity.ok(avaliacaoService.listarPendentes(pageable));
    }

    @GetMapping("/historico")
    public ResponseEntity<Page<AvaliacaoHistoricoResponse>> historico(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("finalizadaEm").descending());
        return ResponseEntity.ok(avaliacaoService.listarHistorico(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoDetalheResponse> detalhe(@PathVariable String id) {
        return ResponseEntity.ok(avaliacaoService.getDetalhe(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AvaliacaoDetalheResponse> atualizar(
            @PathVariable String id,
            @Valid @RequestBody FinalizarAvaliacaoRequest request) {
        return ResponseEntity.ok(avaliacaoService.atualizar(id, request));
    }

    @GetMapping("/by-paciente/{pacienteId}")
    public ResponseEntity<AvaliacaoDetalheResponse> detalhePorPaciente(@PathVariable String pacienteId) {
        return ResponseEntity.ok(avaliacaoService.getDetalheByPaciente(pacienteId));
    }
}
