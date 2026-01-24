package com.thalia.fisioterapia.infra.mongo.adapter;

import com.thalia.fisioterapia.application.ports.out.LeadRepositoryPort;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.infra.mongo.document.LeadDocument;
import com.thalia.fisioterapia.infra.mongo.repository.LeadMongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
@Repository("leadRepositoryMongoAdapter")
public class LeadRepositoryMongoAdapter implements LeadRepositoryPort {

    private final LeadMongoRepository mongoRepository;

    public LeadRepositoryMongoAdapter(LeadMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Lead salvar(Lead lead) {
        LeadDocument saved = mongoRepository.save(
                LeadDocument.fromDomain(lead)
        );
        return saved.toDomain();
    }

    @Override
    public boolean existePorEmail(String email) {
        return mongoRepository.existsByEmail(email);
    }
}
