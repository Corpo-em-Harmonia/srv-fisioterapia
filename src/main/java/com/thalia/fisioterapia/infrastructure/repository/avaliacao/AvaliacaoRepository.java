package com.thalia.fisioterapia.infrastructure.repository.avaliacao;


import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.avaliacao.AvaliacaoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AvaliacaoRepository extends MongoRepository<Avaliacao, String> {

    Optional<Avaliacao> findByPacienteIdAndStatus(String pacienteId, AvaliacaoStatus status);

    Optional<Avaliacao> findFirstByPacienteIdOrderByCriadaEmDesc(String pacienteId);

    Page<Avaliacao> findByStatus(AvaliacaoStatus status, Pageable pageable);
}
