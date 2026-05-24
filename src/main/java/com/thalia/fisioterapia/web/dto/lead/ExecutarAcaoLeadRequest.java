package com.thalia.fisioterapia.web.dto.lead;

import com.thalia.fisioterapia.domain.lead.LeadAcao;
import jakarta.validation.constraints.NotNull;

public record ExecutarAcaoLeadRequest(
        @NotNull(message = "Ação é obrigatória") LeadAcao acao
) {}
