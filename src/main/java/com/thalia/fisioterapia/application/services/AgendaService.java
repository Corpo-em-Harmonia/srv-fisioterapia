package com.thalia.fisioterapia.application.services;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.sessao.*;
import com.thalia.fisioterapia.infra.repository.LeadRepository;
import com.thalia.fisioterapia.infra.repository.SessaoRepository;
import org.springframework.stereotype.Service;

@Service
public class AgendaService {

    private final LeadRepository leadRepository;
    private final SessaoRepository sessaoRepository;

    public AgendaService(LeadRepository leadRepository, SessaoRepository sessaoRepository) {
        this.leadRepository = leadRepository;
        this.sessaoRepository = sessaoRepository;
    }

    public Sessao agendarAvaliacao(String leadId, java.time.Instant dataHora) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead não encontrado"));

        // regra do domínio
        lead.marcarComoAgendado();
        leadRepository.save(lead);

        // cria sessão de avaliação
        Sessao sessao = new Sessao(leadId, SessaoTipo.AVALIACAO, dataHora);
        return sessaoRepository.save(sessao);
    }
}
