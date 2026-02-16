package com.thalia.fisioterapia.web.dto.sessao;


import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RemarcarSessaoRequest(
        @NotNull Instant dataHora
) {}
