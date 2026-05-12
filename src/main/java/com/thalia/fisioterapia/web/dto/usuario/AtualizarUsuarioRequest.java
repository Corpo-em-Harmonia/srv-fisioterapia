package com.thalia.fisioterapia.web.dto.usuario;

import com.thalia.fisioterapia.domain.usuario.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres") String nome,
        @Email(message = "E-mail inválido") @NotBlank(message = "E-mail é obrigatório") @Size(max = 150) String email,
        @NotNull(message = "Perfil é obrigatório") Role role
) {}
