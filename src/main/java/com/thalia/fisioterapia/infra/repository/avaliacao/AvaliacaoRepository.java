package com.thalia.fisioterapia.infra.repository.avaliacao;


import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.avaliacao.AvaliacaoStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AvaliacaoRepository extends MongoRepository<Avaliacao, String> {

    Optional<Avaliacao> findByPacienteIdAndStatus(String pacienteId, AvaliacaoStatus status);

    Optional<Avaliacao> findFirstByPacienteIdOrderByCriadaEmDesc(String pacienteId);
}
