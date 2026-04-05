package com.thalia.fisioterapia.domain.sessao;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class SessaoTest {

    private Sessao sessaoAvaliacao() {
        return new Sessao("lead-1", SessaoTipo.AVALIACAO, Instant.now().plus(1, ChronoUnit.HOURS), null);
    }

    private Sessao sessaoComum() {
        return new Sessao("pac-1", "aval-1", Instant.now().plus(1, ChronoUnit.HOURS), null);
    }

    @Test
    void deveCriarSessaoComStatusMarcada() {
        Sessao s = sessaoAvaliacao();
        assertThat(s.getStatus()).isEqualTo(SessaoStatus.MARCADA);
        assertThat(s.getTipo()).isEqualTo(SessaoTipo.AVALIACAO);
    }

    @Test
    void deveMarcarComparecimento() {
        Sessao s = sessaoComum();
        s.marcarComparecimento();
        assertThat(s.getStatus()).isEqualTo(SessaoStatus.COMPARECEU);
    }

    @Test
    void deveMarcarFaltou() {
        Sessao s = sessaoComum();
        s.marcarFaltou();
        assertThat(s.getStatus()).isEqualTo(SessaoStatus.FALTOU);
    }

    @Test
    void deveCancelar() {
        Sessao s = sessaoComum();
        s.cancelar();
        assertThat(s.getStatus()).isEqualTo(SessaoStatus.CANCELADA);
    }

    @Test
    void deveRemarcar() {
        Sessao s = sessaoComum();
        Instant novaData = Instant.now().plus(2, ChronoUnit.HOURS);
        s.remarcar(novaData);
        assertThat(s.getStatus()).isEqualTo(SessaoStatus.REMARCADA);
        assertThat(s.getDataHora()).isEqualTo(novaData);
    }

    @Test
    void deveRejeitarAcaoEmSessaoCancelada() {
        Sessao s = sessaoComum();
        s.cancelar();
        assertThatThrownBy(s::marcarComparecimento)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void deveRejeitarAcaoEmSessaoCanceladaParaFaltou() {
        Sessao s = sessaoComum();
        s.cancelar();
        assertThatThrownBy(s::marcarFaltou)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveMarcarComparecimentoAvaliacaoParaAguardando() {
        Sessao s = sessaoAvaliacao();
        s.marcarComparecimentoAvaliacao();
        assertThat(s.getStatus()).isEqualTo(SessaoStatus.AGUARDANDO_AVALIACAO);
    }

    @Test
    void deveMarcarAvaliada() {
        Sessao s = sessaoAvaliacao();
        s.marcarComparecimentoAvaliacao();
        s.marcarAvaliada();
        assertThat(s.getStatus()).isEqualTo(SessaoStatus.AVALIADA);
    }

    @Test
    void deveRejeitarMarcarAvaliadaSemAguardar() {
        Sessao s = sessaoAvaliacao();
        assertThatThrownBy(s::marcarAvaliada)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveRejeitarComparecimentoAvaliacaoEmSessaoComum() {
        Sessao s = sessaoComum();
        assertThatThrownBy(s::marcarComparecimentoAvaliacao)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("avaliações");
    }
}
