package com.thalia.fisioterapia.web.dto.usuario;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioResponse(
        String id,
        String nome,
        String email,
        String role,
        List<String> roles,
        boolean ativo,
        LocalDateTime criadoEm
) {}
