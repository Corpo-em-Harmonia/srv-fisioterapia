package com.thalia.fisioterapia.services;

import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.dto.IniciarAvaliacaoRequest;
import com.thalia.fisioterapia.infra.repository.AvaliacaoRepository;
import com.thalia.fisioterapia.infra.repository.LeadRepository;
import org.springframework.stereotype.Service;

@Service
public class IniciarAvaliacaoService {

    private final LeadRepository leadRepository;
    private final AvaliacaoRepository avaliacaoRepository;


    public IniciarAvaliacaoService(
            LeadRepository leadRepository,
            AvaliacaoRepository avaliacaoRepository
    ) {
        this.leadRepository = leadRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public void iniciarAvaliacao(IniciarAvaliacaoRequest request) {

        Lead lead = leadRepository.findById(request.getLeadId())
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado"));

        lead.iniciarAvaliacao();
        leadRepository.save(lead);

        Avaliacao avaliacao = Avaliacao.iniciar(lead.getId());
        avaliacaoRepository.save(avaliacao);
    }
}
