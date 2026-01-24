package com.thalia.fisioterapia.application.ports.out;

import com.thalia.fisioterapia.domain.lead.Lead;



public interface LeadRepositoryPort {
    boolean existePorEmail(String email);
    Lead salvar(Lead lead);
}

