package com.thalia.fisioterapia.web.controllers;

import com.thalia.fisioterapia.application.services.SessaoService;
import com.thalia.fisioterapia.web.dto.RemarcarSessaoRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/sessoes")
public class SessaoController {

    private final SessaoService sessaoService;

    public SessaoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @PostMapping("/{id}/compareceu")
    public ResponseEntity<?> compareceu(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.marcarCompareceu(id));
    }

    @PostMapping("/{id}/faltou")
    public ResponseEntity<?> faltou(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.marcarFaltou(id));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable String id) {
        return ResponseEntity.ok(sessaoService.cancelar(id));
    }

    @PostMapping("/{id}/remarcar")
    public ResponseEntity<?> remarcar(@PathVariable String id, @RequestBody RemarcarSessaoRequest req) {
        return ResponseEntity.ok(sessaoService.remarcar(id, req.novaDataHora()));
    }
}
