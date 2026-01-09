package com.thalia.fisioterapia.controllers;

import com.thalia.fisioterapia.dto.FinalizarAvaliacaoRequest;
import com.thalia.fisioterapia.dto.IniciarAvaliacaoRequest;
import com.thalia.fisioterapia.services.FinalizarAvaliacaoService;
import com.thalia.fisioterapia.services.IniciarAvaliacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avaliacao")
public class AvaliacaoController {

    private final IniciarAvaliacaoService iniciarAvaliacaoService;
    private final FinalizarAvaliacaoService finalizarAvaliacaoService;

    public AvaliacaoController(
            IniciarAvaliacaoService iniciarAvaliacaoService,
            FinalizarAvaliacaoService finalizarAvaliacaoService
    ) {
        this.iniciarAvaliacaoService = iniciarAvaliacaoService;
        this.finalizarAvaliacaoService = finalizarAvaliacaoService;
    }

    @PostMapping("/iniciar")
    public ResponseEntity<Void> iniciar(
            @RequestBody IniciarAvaliacaoRequest request
    ) {
        iniciarAvaliacaoService.iniciarAvaliacao(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/finalizar")
    public ResponseEntity<Void> finalizar(
            @RequestBody FinalizarAvaliacaoRequest request
    ) {
        finalizarAvaliacaoService.finalizar(request);
        return ResponseEntity.ok().build();
    }
}