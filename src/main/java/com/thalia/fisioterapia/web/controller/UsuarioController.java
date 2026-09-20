package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.UsuarioService;
import com.thalia.fisioterapia.web.dto.usuario.AtualizarUsuarioRequest;
import com.thalia.fisioterapia.web.dto.usuario.CriarUsuarioRequest;
import com.thalia.fisioterapia.web.dto.usuario.ResetSenhaRequest;
import com.thalia.fisioterapia.web.dto.usuario.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // RECEPCIONISTA recebe apenas os pacientes (filtro aplicado no service).
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    // RECEPCIONISTA só pode criar PACIENTE — regra validada em UsuarioService.criar.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable String id,
                                                      @Valid @RequestBody AtualizarUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> alternarStatus(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.alternarStatus(id));
    }

    // RECEPCIONISTA só pode redefinir senha de PACIENTE — validado em UsuarioService.resetSenha.
    @PostMapping("/{id}/reset-senha")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<Void> resetSenha(@PathVariable String id,
                                           @Valid @RequestBody ResetSenhaRequest request) {
        usuarioService.resetSenha(id, request);
        return ResponseEntity.noContent().build();
    }
}
