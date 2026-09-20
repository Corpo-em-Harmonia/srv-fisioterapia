package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.PlanoForaValidadeException;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

final class AgendaUtil {

    static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");
    static final LocalTime INICIO_ATENDIMENTO = LocalTime.of(8, 0);
    static final LocalTime FIM_ATENDIMENTO = LocalTime.of(20, 0);
    static final int VALIDADE_GUIA_PADRAO_DIAS = 30;
    static final int MAX_POR_HORARIO = 6;
    static final List<SessaoStatus> STATUS_CONFLITO = List.of(
            SessaoStatus.MARCADA,
            SessaoStatus.REMARCADA,
            SessaoStatus.AGUARDANDO_AVALIACAO
    );

    private AgendaUtil() {}

    static void validarJanela(LocalDateTime dataHora) {
        DayOfWeek dia = dataHora.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new BusinessException("Não é permitido agendar sessões nos fins de semana");
        }
        LocalTime horario = dataHora.toLocalTime();
        if (horario.isBefore(INICIO_ATENDIMENTO) || !horario.isBefore(FIM_ATENDIMENTO)) {
            throw new BusinessException("Horário fora da janela de atendimento (08h–20h)");
        }
    }

    static void validarJanela(Instant dataHora) {
        ZonedDateTime zdt = dataHora.atZone(ZONE_SP);
        DayOfWeek dia = zdt.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new BusinessException("Não é permitido remarcar sessões para fins de semana");
        }
        LocalTime horario = zdt.toLocalTime();
        if (horario.isBefore(INICIO_ATENDIMENTO) || !horario.isBefore(FIM_ATENDIMENTO)) {
            throw new BusinessException("Horário fora da janela de atendimento (08h–20h)");
        }
    }

    static void validarPlano(int quantidade, int frequencia, int validade) {
        int duracao = calcularDuracaoDias(quantidade, frequencia);
        if (duracao > validade) {
            throw new PlanoForaValidadeException(
                    "Plano não cabe na validade da guia",
                    duracao,
                    validade,
                    calcularFrequenciaMinima(quantidade, validade)
            );
        }
    }

    static int calcularDuracaoDias(int quantidade, int frequencia) {
        return ((quantidade + frequencia - 1) / frequencia) * 7;
    }

    static int calcularFrequenciaMinima(int quantidade, int validade) {
        for (int f = 1; f <= 7; f++) {
            if (calcularDuracaoDias(quantidade, f) <= validade) return f;
        }
        return 7;
    }

    static List<LocalDateTime> gerarDatas(
            LocalDateTime primeira,
            int quantidade,
            int frequencia,
            Set<DayOfWeek> diasPreferidos
    ) {
        List<LocalDateTime> resultado = new ArrayList<>();
        LocalTime horario = primeira.toLocalTime();
        LocalDate inicio = primeira.toLocalDate();
        LocalDate cursor = inicio;

        while (resultado.size() < quantidade) {
            LocalDate fim = cursor.plusDays(6);
            List<LocalDate> preferidos = new ArrayList<>();
            List<LocalDate> fallback = new ArrayList<>();

            for (LocalDate dia = cursor; !dia.isAfter(fim); dia = dia.plusDays(1)) {
                if (dia.isBefore(inicio)) continue;
                if (!diasPreferidos.isEmpty() && diasPreferidos.contains(dia.getDayOfWeek())) {
                    preferidos.add(dia);
                } else {
                    fallback.add(dia);
                }
            }

            preferidos.sort(Comparator.naturalOrder());
            fallback.sort(Comparator.naturalOrder());

            int restantes = frequencia;
            for (LocalDate data : preferidos) {
                if (restantes == 0 || resultado.size() == quantidade) break;
                resultado.add(LocalDateTime.of(data, horario));
                restantes--;
            }
            for (LocalDate data : fallback) {
                if (restantes == 0 || resultado.size() == quantidade) break;
                resultado.add(LocalDateTime.of(data, horario));
                restantes--;
            }
            cursor = cursor.plusDays(7);
        }
        return resultado;
    }
}
