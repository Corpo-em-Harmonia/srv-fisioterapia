package com.thalia.fisioterapia.domain.avaliacao;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "avaliacoes")
@Getter
public class Avaliacao {

    @Id
    private String id;

    private String pacienteId;

    private AvaliacaoStatus status;
    private LocalDateTime criadaEm;
    private LocalDateTime finalizadaEm;

    private FichaClinica fichaClinica;

    private Avaliacao() {}

    private Avaliacao(String pacienteId) {
        this.pacienteId = pacienteId;
        this.status = AvaliacaoStatus.AGUARDANDO;
        this.criadaEm = LocalDateTime.now();
    }

    public static Avaliacao criarParaPaciente(String pacienteId) {
        return new Avaliacao(pacienteId);
    }

    public void iniciar() {
        if (this.status != AvaliacaoStatus.AGUARDANDO) {
            throw new IllegalStateException("Avaliação já iniciada ou finalizada");
        }
        this.status = AvaliacaoStatus.EM_ATENDIMENTO;
    }

    public void finalizar(FichaClinica ficha) {
        if (this.status != AvaliacaoStatus.EM_ATENDIMENTO) {
            throw new IllegalStateException("Atendimento não iniciado");
        }
        this.fichaClinica = ficha;
        this.status = AvaliacaoStatus.FINALIZADA;
        this.finalizadaEm = LocalDateTime.now();
    }
}
