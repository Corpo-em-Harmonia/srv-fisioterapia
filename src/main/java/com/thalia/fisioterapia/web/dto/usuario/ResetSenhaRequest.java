package com.thalia.fisioterapia.web.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetSenhaRequest(
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6, max = 200, message = "Senha deve ter entre 6 e 200 caracteres")
        String novaSenha
) {}
