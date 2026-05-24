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

    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_POR_HORARIO = 6;
    private static final List<LocalTime> HORARIOS_ATENDIMENTO = gerarHorarios();

    private final SessaoRepository sessaoRepository;

    public AgendamentoController(SessaoRepository sessaoRepository) {
        this.sessaoRepository = sessaoRepository;
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<List<DisponibilidadeResponse>> disponibilidade(
            @RequestParam("date") LocalDate date,
            @RequestParam(value = "excludeId", required = false) String excludeId) {

        DayOfWeek diaSemana = date.getDayOfWeek();
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return ResponseEntity.ok(List.of());
        }

        List<DisponibilidadeResponse> resp = new ArrayList<>();

        for (LocalTime h : HORARIOS_ATENDIMENTO) {
            Instant dataHora = ZonedDateTime.of(date, h, ZONE_SP).toInstant();

            long count = sessaoRepository.findByDataHoraAndStatusIn(
                    dataHora,
                    List.of(SessaoStatus.MARCADA, SessaoStatus.REMARCADA, SessaoStatus.AGUARDANDO_AVALIACAO)
            ).stream()
                    .filter(s -> excludeId == null || !excludeId.equals(s.getId()))
                    .count();

            resp.add(new DisponibilidadeResponse(
                    String.format("%02d:%02d", h.getHour(), h.getMinute()),
                    count < MAX_POR_HORARIO
            ));
        }

        return ResponseEntity.ok(resp);
    }

    // 08:00–19:00 de 30 em 30 min, sem 12:00–12:30 (almoço)
    private static List<LocalTime> gerarHorarios() {
        List<LocalTime> horarios = new ArrayList<>();
        LocalTime cursor = LocalTime.of(8, 0);
        LocalTime almoco = LocalTime.of(12, 0);
        LocalTime fimAlmoco = LocalTime.of(13, 0);
        LocalTime fim = LocalTime.of(19, 0);

        while (!cursor.isAfter(fim)) {
            if (cursor.isBefore(almoco) || !cursor.isBefore(fimAlmoco)) {
                horarios.add(cursor);
            }
            cursor = cursor.plusMinutes(30);
        }
        return List.copyOf(horarios);
    }
}
