package com.thalia.fisioterapia.services;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.dto.CriarLeadRequest;
import com.thalia.fisioterapia.infra.repository.LeadRepository;
import org.springframework.stereotype.Service;

@Service
public class CriarLeadService {

    private final LeadRepository repository;

    public CriarLeadService(LeadRepository repository) {
        this.repository = repository;
    }

    public Lead criar(CriarLeadRequest request) {

        if (repository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Lead já existe");
        }

        Lead lead = new Lead(
                request.getNome(),
                request.getSobrenome(),
                request.getEmail(),
                request.getCelular(),
                request.getMotivo()
        );

        return repository.save(lead);
    }
}
