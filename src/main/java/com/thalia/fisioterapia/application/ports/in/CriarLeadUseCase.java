package com.thalia.fisioterapia.application.ports.in;

import com.thalia.fisioterapia.domain.lead.Lead;

public interface CriarLeadUseCase {
    Lead criar(CriarLeadCommand command);
}
