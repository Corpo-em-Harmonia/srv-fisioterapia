package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.AgendaConflictException;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Regras de ocupação da agenda (máx. por horário e uma sessão por fisioterapeuta), usadas ao
 * agendar avaliação, agendar sessões e remarcar. Antes essa validação estava copiada em três
 * services, e o "confere e depois salva" não era protegido: duas pessoas agendando no mesmo
 * segundo passavam as duas pela checagem e o horário ficava com 7 sessões.
 *
 * <p>{@link #executar} serializa as operações de agenda com uma trava e, quando há gerenciador de
 * transação, roda tudo numa transação que só termina antes de soltar a trava — assim a próxima
 * operação já enxerga o que foi gravado. A trava vale para uma instância do back-end (o deploy
 * atual); com várias instâncias seria preciso uma trava no banco.
 */
@Component
public class AgendaGuard {

    private final ReentrantLock trava = new ReentrantLock(true);

    private final SessaoRepository sessaoRepository;
    private final PacienteRepository pacienteRepository;
    private final LeadRepository leadRepository;
    private final TransactionTemplate transacao;

    @Autowired
    public AgendaGuard(SessaoRepository sessaoRepository,
                       PacienteRepository pacienteRepository,
                       LeadRepository leadRepository,
                       ObjectProvider<PlatformTransactionManager> transactionManager) {
        this.sessaoRepository = sessaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.leadRepository = leadRepository;
        PlatformTransactionManager tm = transactionManager.getIfAvailable();
        this.transacao = tm != null ? new TransactionTemplate(tm) : null;
    }

    /** Sem transação — usado nos testes de unidade. */
    AgendaGuard(SessaoRepository sessaoRepository, PacienteRepository pacienteRepository, LeadRepository leadRepository) {
        this.sessaoRepository = sessaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.leadRepository = leadRepository;
        this.transacao = null;
    }

    public <T> T executar(Supplier<T> operacao) {
        trava.lock();
        try {
            return transacao != null ? transacao.execute(status -> operacao.get()) : operacao.get();
        } finally {
            trava.unlock();
        }
    }

    /**
     * Lança {@link AgendaConflictException} se o horário estiver lotado ou se a fisioterapeuta já
     * tiver atendimento nele. {@code idsIgnorados}: sessões da própria operação (ex.: a série que
     * está sendo remarcada), que não contam como conflito.
     */
    public void validarHorarioLivre(Instant dataHora, String fisioterapeutaId, Set<String> idsIgnorados) {
        List<Sessao> ocupadas = sessaoRepository.findByDataHoraAndStatusIn(dataHora, AgendaUtil.STATUS_CONFLITO).stream()
                .filter(s -> !idsIgnorados.contains(s.getId()))
                .toList();

        if (fisioterapeutaId != null && !fisioterapeutaId.isBlank()
                && ocupadas.stream().anyMatch(s -> fisioterapeutaId.equals(s.getFisioterapeutaId()))) {
            throw new AgendaConflictException("Fisioterapeuta já possui um atendimento nesse horário", List.of());
        }

        if (ocupadas.size() >= AgendaUtil.MAX_POR_HORARIO) {
            Map<String, String> nomes = nomesDasPessoas(ocupadas);
            List<AgendaConflictException.ConflitoAgendaItem> itens = ocupadas.stream()
                    .map(s -> new AgendaConflictException.ConflitoAgendaItem(
                            s.getId(), s.getDataHora(), nomes.getOrDefault(s.getId(), "Paciente")))
                    .toList();
            throw new AgendaConflictException("Já existe sessão nesse horário", itens);
        }
    }

    /** Nome de quem ocupa cada sessão, buscando pacientes e leads em lote. */
    private Map<String, String> nomesDasPessoas(List<Sessao> sessoes) {
        Set<String> pacienteIds = sessoes.stream().map(Sessao::getPacienteId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> leadIds = sessoes.stream().map(Sessao::getLeadId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<String, String> pacientes = new HashMap<>();
        pacienteRepository.findAllById(pacienteIds).forEach(p -> pacientes.put(p.getId(), nomeOu(p.getNome(), "Paciente")));
        Map<String, String> leads = new HashMap<>();
        leadRepository.findAllById(leadIds).forEach(l -> leads.put(l.getId(), nomeOu(l.getNome(), "Lead")));

        Map<String, String> porSessao = new HashMap<>();
        for (Sessao s : sessoes) {
            String nome = s.getPacienteId() != null
                    ? pacientes.getOrDefault(s.getPacienteId(), "Paciente")
                    : s.getLeadId() != null ? leads.getOrDefault(s.getLeadId(), "Lead") : "Paciente";
            porSessao.put(s.getId(), nome);
        }
        return porSessao;
    }

    private static String nomeOu(String nome, String padrao) {
        return nome != null ? nome : padrao;
    }
}
