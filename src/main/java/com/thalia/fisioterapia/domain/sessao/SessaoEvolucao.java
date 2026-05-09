package com.thalia.fisioterapia.domain.sessao;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class SessaoEvolucao {

    private String observacoes;
    private Integer nivelDor;
    private Integer mobilidade;
    private List<String> exercicios;
    private Instant registradoEm;

    protected SessaoEvolucao() {}

    public SessaoEvolucao(String observacoes, Integer nivelDor, Integer mobilidade, List<String> exercicios) {
        this.observacoes = observacoes;
        this.nivelDor = nivelDor;
        this.mobilidade = mobilidade;
        this.exercicios = exercicios != null ? exercicios : List.of();
        this.registradoEm = Instant.now();
    }
}
