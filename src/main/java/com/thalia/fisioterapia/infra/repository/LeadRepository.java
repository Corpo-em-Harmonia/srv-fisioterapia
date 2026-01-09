package com.thalia.fisioterapia.infra.repository;

import com.thalia.fisioterapia.domain.lead.Lead;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends MongoRepository<Lead, String> {
    boolean existsByEmail(String email);
}


