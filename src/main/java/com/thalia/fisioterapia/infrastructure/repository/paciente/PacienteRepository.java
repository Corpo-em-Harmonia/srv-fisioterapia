package com.thalia.fisioterapia.infrastructure.repository.paciente;

import com.thalia.fisioterapia.domain.paciente.Paciente;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface  PacienteRepository  extends MongoRepository<Paciente, String> {

    boolean existsByLeadId(String leadId);

}
