package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.PacienteService;
import com.thalia.fisioterapia.web.dto.paciente.PacienteAtivoResponse;
import com.thalia.fisioterapia.web.dto.sessao.AgendarSessoesRequest;
import com.thalia.fisioterapia.web.dto.sessao.AgendarSessoesResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<PacienteAtivoResponse>> ativos() {
        return ResponseEntity.ok(pacienteService.listarAtivos());
    }

    @PostMapping("/{id}/agendar-sessoes")
    public ResponseEntity<AgendarSessoesResponse> agendarSessoes(
            @PathVariable String id,
            @Valid @RequestBody AgendarSessoesRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.agendarSessoes(id, request));
    }
}
