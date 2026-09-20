package com.thalia.fisioterapia.domain.lead;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LeadTest {

    private Lead novoLead() {
        return new Lead("Ana", "Lima", "ana@email.com", "11999999999", "observacao");
    }

    @Test
    void deveCriarLeadComStatusNovo() {
        Lead lead = novoLead();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.NOVO);
    }

    @Test
    void deveRegistrarContato() {
        Lead lead = novoLead();
        lead.registrarContato();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.CONTATADO);
    }

    @Test
    void deveRejeitarRegistrarContatoSeNaoForNovo() {
        Lead lead = novoLead();
        lead.registrarContato();
        assertThatThrownBy(lead::registrarContato)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveAgendarAPartirDeNovo() {
        Lead lead = novoLead();
        lead.marcarComoAgendado();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.AGENDADO);
    }

    @Test
    void deveAgendarAPartirDeContatado() {
        Lead lead = novoLead();
        lead.registrarContato();
        lead.marcarComoAgendado();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.AGENDADO);
    }

    @Test
    void deveRejeitarAgendarSeJaAgendado() {
        Lead lead = novoLead();
        lead.marcarComoAgendado();
        assertThatThrownBy(lead::marcarComoAgendado)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveMarcaComoPerdido() {
        Lead lead = novoLead();
        lead.marcarComoPerdido();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.PERDIDO);
    }

    @Test
    void deveIgnorarMarcarComoPerdidoSeJaPerdido() {
        Lead lead = novoLead();
        lead.marcarComoPerdido();
        assertThatNoException().isThrownBy(lead::marcarComoPerdido);
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.PERDIDO);
    }

    @Test
    void deveIncrementarFaltas() {
        Lead lead = novoLead();
        lead.incrementarFaltas();
        lead.incrementarFaltas();
        assertThat(lead.getTotalFaltas()).isEqualTo(2);
    }

    @Test
    void deveIncrementarComparecimentos() {
        Lead lead = novoLead();
        lead.incrementarComparecimentos();
        assertThat(lead.getTotalComparecimentos()).isEqualTo(1);
    }
}
