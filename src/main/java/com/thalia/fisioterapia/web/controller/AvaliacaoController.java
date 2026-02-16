package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.web.dto.avaliacao.FinalizarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.avaliacao.IniciarAvaliacaoRequest;
import com.thalia.fisioterapia.application.service.AvaliacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avaliacao")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(

            AvaliacaoService avaliacaoService
    ) {
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
}