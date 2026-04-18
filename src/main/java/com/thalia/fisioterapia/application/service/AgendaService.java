package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import org.springframework.stereotype.Service;

@Service
public class AgendaService {

    private final LeadRepository leadRepository;
    private final SessaoRepository sessaoRepository;

    public AgendaService(LeadRepository leadRepository, SessaoRepository sessaoRepository) {
        this.leadRepository = leadRepository;
        this.sessaoRepository = sessaoRepository;
    }

}
