package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infra.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.web.dto.sessao.DisponibilidadeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
public class AgendaService {

    private static final List<LocalTime> HORARIOS = List.of(
            LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0),
            LocalTime.of(11, 0), LocalTime.of(13, 0), LocalTime.of(14, 0),
            LocalTime.of(15, 0), LocalTime.of(16, 0), LocalTime.of(17, 0)
    );

    private final SessaoRepository sessaoRepository;

    @Value("${app.timezone:America/Sao_Paulo}")
    private String timezone;

    public AgendaService(SessaoRepository sessaoRepository) {
        this.sessaoRepository = sessaoRepository;
    }

    public List<DisponibilidadeResponse> obterDisponibilidade(LocalDate date) {
        ZoneId zone = ZoneId.of(timezone);
        return HORARIOS.stream()
                .map(h -> {
                    Instant dataHora = ZonedDateTime.of(date, h, zone).toInstant();
                    boolean ocupado = sessaoRepository.existsByDataHoraAndStatusIn(
                            dataHora, List.of(SessaoStatus.MARCADA, SessaoStatus.REMARCADA));
                    return new DisponibilidadeResponse(h.toString(), !ocupado);
                })
                .toList();
    }
}
