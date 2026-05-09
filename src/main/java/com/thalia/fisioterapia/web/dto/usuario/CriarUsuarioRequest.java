package com.thalia.fisioterapia.web.dto.usuario;

import com.thalia.fisioterapia.domain.usuario.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank String nome,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres") String senha,
        @NotNull Role role
) {}
