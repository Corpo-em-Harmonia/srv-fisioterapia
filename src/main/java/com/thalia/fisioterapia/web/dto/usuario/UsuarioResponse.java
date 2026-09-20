package com.thalia.fisioterapia.web.dto.usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        String id,
        String nome,
        String email,
        String role,
        boolean ativo,
        LocalDateTime criadoEm
) {}
