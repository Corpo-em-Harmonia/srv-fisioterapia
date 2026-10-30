package com.thalia.fisioterapia.infrastructure.repository.paciente;

import com.thalia.fisioterapia.domain.paciente.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface  PacienteRepository  extends MongoRepository<Paciente, String> {

    boolean existsByLeadId(String leadId);

    Page<Paciente> findByFisioterapeutaId(String fisioterapeutaId, Pageable pageable);

    /** Vincula a conta PACIENTE ao cadastro (o e-mail do lead vira o e-mail do usuário). */
    Optional<Paciente> findFirstByEmailIgnoreCase(String email);

}
