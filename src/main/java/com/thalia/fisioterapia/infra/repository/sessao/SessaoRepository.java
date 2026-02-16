package com.thalia.fisioterapia.infra.repository.sessao;

import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SessaoRepository extends MongoRepository<Sessao, String> {

    List<Sessao> findByDataHoraBetween(Instant start, Instant end);
    List<Sessao> findByDataHoraBetweenOrderByDataHoraAsc(Instant start, Instant end);

    boolean existsByDataHoraAndStatusIn(Instant dataHora, Collection<SessaoStatus> status);

    Optional<Sessao> findByLeadIdAndStatusInOrderByDataHoraDesc(
            String leadId, Collection<SessaoStatus> status
    );

}
