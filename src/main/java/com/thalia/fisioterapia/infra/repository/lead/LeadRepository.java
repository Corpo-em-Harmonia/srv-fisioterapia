package com.thalia.fisioterapia.infra.repository.lead;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.lead.LeadStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LeadRepository extends MongoRepository<Lead, String> {
    boolean existsByEmail(String email);
    List<Lead> findByStatusIn(Collection<LeadStatus> status);
}


