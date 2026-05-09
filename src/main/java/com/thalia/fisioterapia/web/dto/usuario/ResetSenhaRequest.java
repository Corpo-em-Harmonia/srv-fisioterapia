package com.thalia.fisioterapia.web.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetSenhaRequest(
        @NotBlank @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres") String novaSenha
) {}
