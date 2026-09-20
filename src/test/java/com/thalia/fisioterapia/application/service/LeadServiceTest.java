package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.PlanoForaValidadeException;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.web.dto.agenda.AgendarAvaliacaoRequest;
import com.thalia.fisioterapia.web.dto.agenda.AgendarAvaliacaoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    private LeadService leadService;

    @BeforeEach
    void setUp() {
        this.leadService = new LeadService(leadRepository, sessaoRepository, pacienteRepository);
    }

    @Test
    void agendarAvaliacaoAvulso_quandoModoAusente_deveCriarUmaSessao() {
        Lead lead = novoLead("l_1");

        when(leadRepository.findById("l_1")).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessaoRepository.findByDataHoraAndStatusIn(any(), any())).thenReturn(List.of());
        when(sessaoRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Object> sessoes = invocation.getArgument(0);
            ReflectionTestUtils.setField(sessoes.get(0), "id", "ss_001");
            return sessoes;
        });

        AgendarAvaliacaoResponse response = leadService.agendarAvaliacao("l_1", request(
                LocalDateTime.of(2026, 4, 23, 14, 0),
                "Primeira avaliacao",
                null,
                null,
                null,
                null
        ));

        assertEquals("avulso", response.modoAgendamento());
        assertEquals(1, response.sessoesGeradas());
        assertEquals("ss_001", response.sessaoInicialId());
        assertEquals("Agendamento criado com sucesso", response.mensagem());

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(sessaoRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("AGENDADO", lead.getStatus().name());
    }

    @Test
    void agendarAvaliacaoRecorrente_quandoPlanoCabeNaValidade_deveCriarSerie() {
        Lead lead = novoLead("l_2");

        when(leadRepository.findById("l_2")).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessaoRepository.findByDataHoraAndStatusIn(any(), any())).thenReturn(List.of());
        when(sessaoRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Object> sessoes = invocation.getArgument(0);
            List<Object> copia = new ArrayList<>(sessoes);
            for (int i = 0; i < copia.size(); i++) {
                ReflectionTestUtils.setField(copia.get(i), "id", "ss_" + (i + 1));
            }
            return copia;
        });

        AgendarAvaliacaoResponse response = leadService.agendarAvaliacao("l_2", request(
                LocalDateTime.of(2026, 4, 23, 14, 0),
                "Plano 8 sessoes",
                "recorrente",
                2,
                8,
                30
        ));

        assertEquals("recorrente", response.modoAgendamento());
        assertEquals(8, response.sessoesGeradas());
        assertNotNull(response.serieId());
        assertTrue(response.serieId().startsWith("sr_"));
        assertEquals("ss_1", response.sessaoInicialId());
    }

    @Test
    void agendarAvaliacaoRecorrente_quandoValidadeAusenteEPlanoNaoCabe_deveLancar422NoServico() {
        Lead lead = novoLead("l_3");

        when(leadRepository.findById("l_3")).thenReturn(Optional.of(lead));

        PlanoForaValidadeException ex = assertThrows(
                PlanoForaValidadeException.class,
                () -> leadService.agendarAvaliacao("l_3", request(
                        LocalDateTime.of(2026, 4, 23, 14, 0),
                        "Plano longo",
                        "recorrente",
                        2,
                        10,
                        null
                ))
        );

        assertEquals("Plano recorrente nao cabe na validade da guia", ex.getMessage());
        assertEquals(35, ex.getDuracaoDias());
        assertEquals(30, ex.getValidadeGuiaDias());
        assertEquals(3, ex.getFrequenciaMinimaSugerida());
    }

    @Test
    void agendarAvaliacaoRecorrente_quandoPlanoNaoCabeNaValidadeInformada_deveSugerirFrequenciaMinima() {
        Lead lead = novoLead("l_4");

        when(leadRepository.findById("l_4")).thenReturn(Optional.of(lead));

        PlanoForaValidadeException ex = assertThrows(
                PlanoForaValidadeException.class,
                () -> leadService.agendarAvaliacao("l_4", request(
                        LocalDateTime.of(2026, 4, 23, 14, 0),
                        "Plano fora da validade",
                        "recorrente",
                        2,
                        8,
                        21
                ))
        );

        assertEquals(28, ex.getDuracaoDias());
        assertEquals(21, ex.getValidadeGuiaDias());
        assertEquals(3, ex.getFrequenciaMinimaSugerida());
    }

    private Lead novoLead(String id) {
        Lead lead = new Lead("Maria", "Silva", "maria@example.com", "11999999999", "obs");
        lead.setId(id);
        return lead;
    }

    private AgendarAvaliacaoRequest request(
            LocalDateTime dataHora,
            String observacao,
            String modoAgendamento,
            Integer frequenciaSemanal,
            Integer quantidadeSessoes,
            Integer validadeGuiaDias
    ) {
        return new AgendarAvaliacaoRequest(
                dataHora,
                observacao,
                modoAgendamento,
                frequenciaSemanal,
                quantidadeSessoes,
                validadeGuiaDias
        );
    }
}
