package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaGuardTest {

    private static final Instant HORARIO = Instant.parse("2026-11-03T13:00:00Z");

    @Mock private SessaoRepository sessaoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private LeadRepository leadRepository;

    private AgendaGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AgendaGuard(sessaoRepository, pacienteRepository, leadRepository);
        lenient().when(pacienteRepository.findAllById(anyIterable())).thenReturn(List.of());
        lenient().when(leadRepository.findAllById(anyIterable())).thenReturn(List.of());
    }

    @Test
    void horarioComVaga_naoLanca() {
        when(sessaoRepository.findByDataHoraAndStatusIn(any(), any())).thenReturn(sessoes(5, null));
        assertThatNoException().isThrownBy(() -> guard.validarHorarioLivre(HORARIO, "fisio-1", Set.of()));
    }

    @Test
    void horarioLotado_lancaConflitoComAsSessoes() {
        when(sessaoRepository.findByDataHoraAndStatusIn(any(), any())).thenReturn(sessoes(AgendaUtil.MAX_POR_HORARIO, null));

        assertThatThrownBy(() -> guard.validarHorarioLivre(HORARIO, null, Set.of()))
                .isInstanceOf(AgendaConflictException.class)
                .satisfies(ex -> assertThat(((AgendaConflictException) ex).getConflitos()).hasSize(AgendaUtil.MAX_POR_HORARIO));
    }

    @Test
    void fisioterapeutaJaOcupada_lancaMesmoComVaga() {
        when(sessaoRepository.findByDataHoraAndStatusIn(any(), any())).thenReturn(sessoes(1, "fisio-1"));

        assertThatThrownBy(() -> guard.validarHorarioLivre(HORARIO, "fisio-1", Set.of()))
                .isInstanceOf(AgendaConflictException.class)
                .hasMessageContaining("Fisioterapeuta");
    }

    @Test
    void sessoesDaPropriaOperacao_naoContamComoConflito() {
        List<Sessao> lotado = sessoes(AgendaUtil.MAX_POR_HORARIO, null);
        when(sessaoRepository.findByDataHoraAndStatusIn(any(), any())).thenReturn(lotado);

        // remarcando a série inteira: a própria sessão (s0) não ocupa a vaga dela mesma
        assertThatNoException().isThrownBy(() -> guard.validarHorarioLivre(HORARIO, null, Set.of("s0")));
    }

    @Test
    void executar_naoDeixaDuasOperacoesDeAgendaRodaremAoMesmoTempo() throws Exception {
        AtomicInteger dentro = new AtomicInteger();
        AtomicInteger maximo = new AtomicInteger();
        int threads = 8;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                largada.await();
                return guard.executar(() -> {
                    maximo.accumulateAndGet(dentro.incrementAndGet(), Math::max);
                    try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    dentro.decrementAndGet();
                    return null;
                });
            });
        }
        largada.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        assertThat(maximo.get()).isEqualTo(1);
    }

    private List<Sessao> sessoes(int quantidade, String fisioterapeutaId) {
        List<Sessao> lista = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            Sessao s = new Sessao("pac-" + i, "aval-" + i, HORARIO, null);
            ReflectionTestUtils.setField(s, "id", "s" + i);
            if (fisioterapeutaId != null) s.atribuirFisioterapeuta(fisioterapeutaId);
            lista.add(s);
        }
        return lista;
    }
}
