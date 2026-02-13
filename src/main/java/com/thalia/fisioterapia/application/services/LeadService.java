package com.thalia.fisioterapia.application.services;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.lead.LeadAcao;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoTipo;
import com.thalia.fisioterapia.infra.repository.LeadRepository;
import com.thalia.fisioterapia.infra.repository.SessaoRepository;
import com.thalia.fisioterapia.web.dto.AgendarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.CriarLeadRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final SessaoRepository sessaoRepository;

    public LeadService(LeadRepository leadRepository, SessaoRepository sessaoRepository) {
        this.leadRepository = leadRepository;
        this.sessaoRepository = sessaoRepository;
    }

    public Lead criar(CriarLeadRequest request) {
        if (request.getEmail() != null && leadRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Lead já existe");
        }

        Lead lead = new Lead(
                request.getNome(),
                request.getSobrenome(),
                request.getEmail(),
                request.getTelefone()
        );

        return leadRepository.save(lead);
    }

    public List<Lead> listar() {
        return leadRepository.findAll();
    }

    @Transactional
    public Lead executarAcaoSimples(String leadId, LeadAcao acao) {
        Lead lead = buscarLead(leadId);

        switch (acao) {
            case REGISTRAR_CONTATO -> lead.registrarContato();
            case AGENDAR_AVALIACAO -> lead.marcarComoAgendado();
            case CANCELAR -> lead.marcarComoPerdido();
        }

        return leadRepository.save(lead);
    }

    @Transactional
    public Sessao agendarAvaliacao(String leadId, AgendarAvaliacaoRequest req) {
        Lead lead = buscarLead(leadId);

        // domínio do lead
        lead.marcarComoAgendado();
        leadRepository.save(lead);

        // cria sessão de AVALIACAO
        Sessao sessao = new Sessao(
                lead.getId(),
                SessaoTipo.AVALIACAO,
                req.dataHora()
        );

        return sessaoRepository.save(sessao);
    }

    private Lead buscarLead(String id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado"));
    }
}
