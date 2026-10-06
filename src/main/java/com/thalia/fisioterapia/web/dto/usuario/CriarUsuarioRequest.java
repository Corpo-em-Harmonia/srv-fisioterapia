package com.thalia.fisioterapia.web.dto.usuario;

import com.thalia.fisioterapia.domain.usuario.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarUsuarioRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 200, message = "Senha deve ter entre 8 e 200 caracteres")
        String senha,

                Role role,

                List<Role> roles
) {
        public CriarUsuarioRequest(String nome, String email, String senha, Role role) {
                this(nome, email, senha, role, null);
        }
}
