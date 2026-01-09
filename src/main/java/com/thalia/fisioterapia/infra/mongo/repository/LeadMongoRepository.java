package com.thalia.fisioterapia.infra.mongo.repository;

import com.thalia.fisioterapia.infra.mongo.document.LeadDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LeadMongoRepository
        extends MongoRepository<LeadDocument, String> {

    boolean existsByEmail(String email);
}
