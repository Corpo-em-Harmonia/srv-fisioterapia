package com.thalia.fisioterapia.infra.repository;

import com.thalia.fisioterapia.domain.paciente.Paciente;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface  PacienteRepository  extends MongoRepository<Paciente, String> {

    boolean existsByLeadId(String leadId);

}
