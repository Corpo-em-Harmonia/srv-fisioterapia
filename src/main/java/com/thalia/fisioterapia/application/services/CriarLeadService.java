package com.thalia.fisioterapia.application.services;

import com.thalia.fisioterapia.application.ports.in.CriarLeadCommand;
import com.thalia.fisioterapia.application.ports.in.CriarLeadUseCase;
import com.thalia.fisioterapia.application.ports.out.LeadRepositoryPort;
import com.thalia.fisioterapia.domain.lead.Lead;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CriarLeadService implements CriarLeadUseCase {

    private final LeadRepositoryPort repository;

    public CriarLeadService(@Qualifier("leadRepositoryMongoAdapter") LeadRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Lead criar(CriarLeadCommand command) {

        if (repository.existePorEmail(command.email())) {
            throw new RuntimeException("Lead já existe");
        }

        Lead lead = new Lead(
                command.nome(),
                command.sobrenome(),
                command.email(),
                command.celular()
        );
        return repository.salvar(lead);
    }
}

