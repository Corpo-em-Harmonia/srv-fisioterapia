package com.thalia.fisioterapia.services;

import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.avaliacao.AvaliacaoStatus;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.dto.FinalizarAvaliacaoRequest;
import com.thalia.fisioterapia.infra.repository.AvaliacaoRepository;
import com.thalia.fisioterapia.infra.repository.LeadRepository;
import com.thalia.fisioterapia.infra.repository.PacienteRepository;
import org.springframework.stereotype.Service;

@Service
public class FinalizarAvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final PacienteRepository pacienteRepository;
    private final LeadRepository leadRepository;

    public FinalizarAvaliacaoService(
            AvaliacaoRepository avaliacaoRepository,
            PacienteRepository pacienteRepository,
            LeadRepository leadRepository
    ) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.leadRepository = leadRepository;
    }
    public void finalizar(FinalizarAvaliacaoRequest request) {

        Avaliacao avaliacao = avaliacaoRepository
                .findByLeadIdAndStatus(
                        request.getLeadId(),
                        AvaliacaoStatus.EM_ANDAMENTO
                )
                .orElseThrow(() ->
                        new IllegalStateException("Avaliação em andamento não encontrada")
                );

        avaliacao.finalizar(
                request.getMedico(),
                request.getHda(),
                request.getHpp(),
                request.getDiagnostico(),
                request.getCondutaTerapeutica(),
                request.getPrognostico()
        );

        avaliacaoRepository.save(avaliacao);

        // 🔹 cria paciente apenas uma vez
        if (!pacienteRepository.existsByLeadId(request.getLeadId())) {
            Lead lead = leadRepository.findById(request.getLeadId())
                    .orElseThrow();

            Paciente paciente = Paciente.fromLead(lead);
            pacienteRepository.save(paciente);
        }
    }
}