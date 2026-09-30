package com.thalia.fisioterapia.infrastructure.repository.lead;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.lead.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface LeadRepository extends MongoRepository<Lead, String> {
    boolean existsByEmail(String email);
    Optional<Lead> findByEmail(String email);
    Page<Lead> findByStatusIn(Collection<LeadStatus> status, Pageable pageable);
}


