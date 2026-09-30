package com.thalia.fisioterapia.infrastructure.repository.sessao;

import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.domain.sessao.SessaoTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SessaoRepository extends MongoRepository<Sessao, String> {

    // Queries existentes
    List<Sessao> findByDataHoraBetween(Instant start, Instant end);
    List<Sessao> findByDataHoraBetweenOrderByDataHoraAsc(Instant start, Instant end);
    boolean existsByDataHoraAndStatusIn(Instant dataHora, Collection<SessaoStatus> status);
    List<Sessao> findByDataHoraAndStatusIn(Instant dataHora, Collection<SessaoStatus> status);
    Optional<Sessao> findByLeadIdAndStatusInOrderByDataHoraDesc(String leadId, Collection<SessaoStatus> status);


    // Sessões pendentes (hoje + atrasadas não confirmadas)
    @Query("{ 'dataHora': { $lte: ?0 }, 'status': { $in: ['MARCADA', 'REMARCADA'] } }")
    List<Sessao> findPendentes(Instant agora);

    @Query("{ 'dataHora': { $lte: ?0 }, 'status': { $in: ['MARCADA', 'REMARCADA'] } }")
    Page<Sessao> findPendentes(Instant agora, Pageable pageable);

    @Query("{ 'dataHora': { $lte: ?0 }, 'status': { $in: ?1 } }")
    Page<Sessao> findPendentesComStatus(Instant agora, Collection<SessaoStatus> status, Pageable pageable);

    // Sessões por período
    List<Sessao> findByDataHoraBetweenAndStatusInOrderByDataHoraAsc(
            Instant start,
            Instant end,
            Collection<SessaoStatus> status
    );

    // Variantes paginadas para as tabelas de agenda (evita carregar a coleção inteira em memória)
    Page<Sessao> findByDataHoraBetween(Instant start, Instant end, Pageable pageable);

    Page<Sessao> findByDataHoraBetweenAndStatusIn(
            Instant start,
            Instant end,
            Collection<SessaoStatus> status,
            Pageable pageable
    );

    Page<Sessao> findByStatus(SessaoStatus status, Pageable pageable);

    // Sessões por tipo
    List<Sessao> findByTipoAndDataHoraBetweenOrderByDataHoraAsc(
            SessaoTipo tipo,
            Instant start,
            Instant end
    );

    // Estatísticas
    long countByDataHoraBetween(Instant start, Instant end);
    long countByStatus(SessaoStatus status);
    long countByDataHoraBetweenAndStatus(Instant start, Instant end, SessaoStatus status);

    // Sessões por paciente
    List<Sessao> findByPacienteIdOrderByDataHoraDesc(String pacienteId);
        List<Sessao> findByPacienteIdOrderByDataHoraAsc(String pacienteId);

    List<Sessao> findByPacienteIdInOrderByDataHoraAsc(Collection<String> pacienteIds);

    // Sessões vinculadas a uma avaliação
    List<Sessao> findByAvaliacaoIdOrderByDataHoraAsc(String avaliacaoId);

    List<Sessao> findBySerieIdOrderByNumeroOcorrenciaAsc(String serieId);

    List<Sessao> findByStatusOrderByDataHoraAsc(SessaoStatus status);

    List<Sessao> findByStatus(SessaoStatus sessaoStatus);
}