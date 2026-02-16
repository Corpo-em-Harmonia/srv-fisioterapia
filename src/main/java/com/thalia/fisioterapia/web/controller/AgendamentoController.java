package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.application.service.AgendaService;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infra.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.web.dto.agenda.AgendarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.sessao.DisponibilidadeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {


    private final SessaoRepository sessaoRepository;

    public AgendamentoController(SessaoRepository sessaoRepository) {
        this.sessaoRepository = sessaoRepository;
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<List<DisponibilidadeResponse>> disponibilidade(@RequestParam("date") LocalDate date) {

        // Horários fixos (você pode externalizar depois)
        List<LocalTime> horarios = List.of(
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
                LocalTime.of(17, 0)
        );

        ZoneId zone = ZoneId.systemDefault();
        List<DisponibilidadeResponse> resp = new ArrayList<>();

        for (LocalTime h : horarios) {
            Instant dataHora = ZonedDateTime.of(date, h, zone).toInstant();

            boolean ocupado = sessaoRepository.existsByDataHoraAndStatusIn(
                    dataHora,
                    List.of(SessaoStatus.MARCADA, SessaoStatus.REMARCADA)
            );

            resp.add(new DisponibilidadeResponse(h.toString(), !ocupado));
        }

        return ResponseEntity.ok(resp);
    }
}



