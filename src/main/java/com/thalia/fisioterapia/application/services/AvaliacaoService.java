package com.thalia.fisioterapia.application.services;

import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.avaliacao.FichaClinica;
import com.thalia.fisioterapia.web.dto.FinalizarAvaliacaoRequest;
import com.thalia.fisioterapia.infra.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;
@Service
public class AvaliacaoService {

    private final AvaliacaoRepository repository;

    public AvaliacaoService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    public void iniciar(String avaliacaoId){

        Avaliacao avaliacao = repository.findById(avaliacaoId)
                .orElseThrow();

        avaliacao.iniciar();

        repository.save(avaliacao);
    }

    public void finalizar(FinalizarAvaliacaoRequest request){

        Avaliacao avaliacao = repository.findById(request.getAvaliacaoId())
                .orElseThrow();

        FichaClinica ficha = new FichaClinica(
                request.getMedico(),
                request.getHda(),
                request.getHpp(),
                request.getDiagnostico(),
                request.getTestesRealizados(),
                request.getGoniometria(),
                request.getCondutaTerapeutica(),
                request.getPrognostico(),
                request.getDesfecho(),
                request.getComodidade(),
                request.getMedicamentos(),
                request.getCirurgia()
        );

        avaliacao.finalizar(ficha);

        repository.save(avaliacao);
    }
}
