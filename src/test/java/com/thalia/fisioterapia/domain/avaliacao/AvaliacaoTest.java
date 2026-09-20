package com.thalia.fisioterapia.domain.avaliacao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AvaliacaoTest {

    private FichaClinica fichaExemplo() {
        return new FichaClinica(
                "Dr. Silva", "dor lombar", "hipertensao",
                "lombalgia", "teste de thomas", "120 graus",
                "exercicios", "bom", "melhora parcial",
                "bom", "ibuprofen", "nao"
        );
    }

    @Test
    void deveCriarAvaliacaoComStatusAguardando() {
        Avaliacao av = Avaliacao.criarParaPaciente("pac-1");
        assertThat(av.getStatus()).isEqualTo(AvaliacaoStatus.AGUARDANDO);
        assertThat(av.getPacienteId()).isEqualTo("pac-1");
    }

    @Test
    void deveIniciarAvaliacao() {
        Avaliacao av = Avaliacao.criarParaPaciente("pac-1");
        av.iniciar();
        assertThat(av.getStatus()).isEqualTo(AvaliacaoStatus.EM_ATENDIMENTO);
    }

    @Test
    void deveRejeitarIniciarSeNaoAguardando() {
        Avaliacao av = Avaliacao.criarParaPaciente("pac-1");
        av.iniciar();
        assertThatThrownBy(av::iniciar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já iniciada ou finalizada");
    }

    @Test
    void deveFinalizarComFichaClinica() {
        Avaliacao av = Avaliacao.criarParaPaciente("pac-1");
        av.iniciar();
        FichaClinica ficha = fichaExemplo();
        av.finalizar(ficha);
        assertThat(av.getStatus()).isEqualTo(AvaliacaoStatus.FINALIZADA);
        assertThat(av.getFichaClinica()).isEqualTo(ficha);
        assertThat(av.getFinalizadaEm()).isNotNull();
    }

    @Test
    void deveRejeitarFinalizarSemIniciar() {
        Avaliacao av = Avaliacao.criarParaPaciente("pac-1");
        assertThatThrownBy(() -> av.finalizar(fichaExemplo()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não iniciado");
    }

    @Test
    void deveTerDataCriacaoPreenchida() {
        Avaliacao av = Avaliacao.criarParaPaciente("pac-1");
        assertThat(av.getCriadaEm()).isNotNull();
    }
}
