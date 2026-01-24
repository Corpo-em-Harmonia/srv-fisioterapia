package com.thalia.fisioterapia.services;

import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.avaliacao.AvaliacaoStatus;
import com.thalia.fisioterapia.dto.FinalizarAvaliacaoRequest;
import com.thalia.fisioterapia.infra.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;

@Service
public class FinalizarAvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;

    public FinalizarAvaliacaoService(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public void finalizar(FinalizarAvaliacaoRequest request) {

        if (request.getLeadId() == null) {
            throw new IllegalArgumentException("leadId é obrigatório");
        }

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
    }
}