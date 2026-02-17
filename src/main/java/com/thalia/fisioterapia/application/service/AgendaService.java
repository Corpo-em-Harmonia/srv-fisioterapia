package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.sessao.*;
import com.thalia.fisioterapia.infra.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infra.repository.sessao.SessaoRepository;
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
