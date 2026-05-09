package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.web.dto.sessao.DisponibilidadeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_POR_HORARIO = 6;

    private final SessaoRepository sessaoRepository;

    public AgendamentoController(SessaoRepository sessaoRepository) {
        this.sessaoRepository = sessaoRepository;
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<List<DisponibilidadeResponse>> disponibilidade(
            @RequestParam("date") LocalDate date,
            @RequestParam(value = "excludeId", required = false) String excludeId) {

        List<LocalTime> horarios = List.of(
                LocalTime.of(8, 0),
                LocalTime.of(8, 30),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                LocalTime.of(13, 0),
                LocalTime.of(13, 30),
                LocalTime.of(14, 0),
                LocalTime.of(14, 30),
                LocalTime.of(15, 0),
                LocalTime.of(15, 30),
                LocalTime.of(16, 0),
                LocalTime.of(16, 30),
                LocalTime.of(17, 0),
                LocalTime.of(17, 30),
                LocalTime.of(18, 0),
                LocalTime.of(18, 30),
                LocalTime.of(19, 0)
        );

        DayOfWeek diaSemana = date.getDayOfWeek();
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return ResponseEntity.ok(List.of());
        }

        List<DisponibilidadeResponse> resp = new ArrayList<>();

        for (LocalTime h : horarios) {
            Instant dataHora = ZonedDateTime.of(date, h, ZONE).toInstant();

            long count = sessaoRepository.findByDataHoraAndStatusIn(
                    dataHora,
                    List.of(SessaoStatus.MARCADA, SessaoStatus.REMARCADA, SessaoStatus.AGUARDANDO_AVALIACAO)
            ).stream()
                    .filter(s -> excludeId == null || !excludeId.equals(s.getId()))
                    .count();

            resp.add(new DisponibilidadeResponse(
                    String.format("%02d:%02d", h.getHour(), h.getMinute()),
                    count < MAX_POR_HORARIO  // disponivel
            ));
        }

        return ResponseEntity.ok(resp);
    }
}
