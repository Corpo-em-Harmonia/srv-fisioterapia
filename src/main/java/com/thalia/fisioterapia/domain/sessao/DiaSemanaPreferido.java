package com.thalia.fisioterapia.domain.sessao;

import java.time.DayOfWeek;
import java.util.Arrays;

public enum DiaSemanaPreferido {
    DOM(DayOfWeek.SUNDAY),
    SEG(DayOfWeek.MONDAY),
    TER(DayOfWeek.TUESDAY),
    QUA(DayOfWeek.WEDNESDAY),
    QUI(DayOfWeek.THURSDAY),
    SEX(DayOfWeek.FRIDAY),
    SAB(DayOfWeek.SATURDAY);

    private final DayOfWeek dayOfWeek;

    DiaSemanaPreferido(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public static DiaSemanaPreferido fromCode(String code) {
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Dia da semana invalido: " + code));
    }
}
