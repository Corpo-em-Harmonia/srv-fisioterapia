package com.thalia.fisioterapia.application.services;

import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.lead.LeadAcao;
import com.thalia.fisioterapia.domain.lead.LeadStatus;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.web.dto.CriarLeadRequest;
import com.thalia.fisioterapia.infra.repository.AvaliacaoRepository;
import com.thalia.fisioterapia.infra.repository.LeadRepository;
import com.thalia.fisioterapia.infra.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.thalia.fisioterapia.domain.lead.LeadAcao.CONFIRMAR_COMPARECIMENTO;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final PacienteRepository pacienteRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public LeadService(LeadRepository repository, PacienteRepository pacienteRepository, AvaliacaoRepository avaliacaoRepository) {
        this.leadRepository = repository;
        this.pacienteRepository = pacienteRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public Lead criar(CriarLeadRequest request) {

        if (leadRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Lead já existe");
        }

        Lead lead = new Lead(
                request.getNome(),
                request.getSobrenome(),
                request.getEmail(),
                request.getTelefone()
        );

        return leadRepository.save(lead);
    }

    public List<Lead> allLead() {
        return leadRepository.findAll();
    }

    @Transactional
    private void confirmarComparecimento(Lead lead){

        if (lead.getStatus() == LeadStatus.CONVERTIDO) {
            throw new IllegalStateException("Lead já convertido em paciente");
        }

        // 1 — vira paciente (regra no domínio)
        Paciente paciente = lead.confirmarComparecimento();
        pacienteRepository.save(paciente);

        // 2 — cria avaliação inicial
        Avaliacao avaliacao = Avaliacao.criarParaPaciente(paciente.getId());
        avaliacaoRepository.save(avaliacao);

        // 3 — salva lead (status já mudou dentro do domínio)
        leadRepository.save(lead);
    }

    @Transactional
    public void executarAcao(String leadId, LeadAcao acao) {

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead não encontrado"));

        switch (acao) {

            case CONFIRMAR_COMPARECIMENTO -> confirmarComparecimento(lead);

            case CANCELAR -> {
                lead.marcarComoPerdido();     // usa regra do domínio
                leadRepository.save(lead);
            }

            case REGISTRAR_CONTATO -> {
                lead.registrarContato();
                leadRepository.save(lead);
            }

            case AGENDAR_AVALIACAO -> {
                lead.agendarAvaliacao();
                leadRepository.save(lead);
            }
        }
    }
}
