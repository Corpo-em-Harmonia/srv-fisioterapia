package com.thalia.fisioterapia.web.dto.usuario;

import com.thalia.fisioterapia.domain.usuario.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarUsuarioRequest(
        @NotBlank String nome,
        @Email @NotBlank String email,
        @NotNull Role role
) {}
