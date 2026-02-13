package com.thalia.fisioterapia.application.services;

import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.domain.sessao.Sessao;
import com.thalia.fisioterapia.domain.sessao.SessaoTipo;
import com.thalia.fisioterapia.infra.repository.AvaliacaoRepository;
import com.thalia.fisioterapia.infra.repository.LeadRepository;
import com.thalia.fisioterapia.infra.repository.PacienteRepository;
import com.thalia.fisioterapia.infra.repository.SessaoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final LeadRepository leadRepository;
    private final PacienteRepository pacienteRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public SessaoService(
            SessaoRepository sessaoRepository,
            LeadRepository leadRepository,
            PacienteRepository pacienteRepository,
            AvaliacaoRepository avaliacaoRepository
    ) {
        this.sessaoRepository = sessaoRepository;
        this.leadRepository = leadRepository;
        this.pacienteRepository = pacienteRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public Sessao remarcar(String sessaoId, Instant novaDataHora) {
        Sessao s = get(sessaoId);
        s.remarcar(novaDataHora);
        return sessaoRepository.save(s);
    }

    public Sessao cancelar(String sessaoId) {
        Sessao s = get(sessaoId);
        s.cancelar();
        return sessaoRepository.save(s);
    }

    public Sessao marcarFaltou(String sessaoId) {
        Sessao s = get(sessaoId);
        s.marcarFaltou();
        return sessaoRepository.save(s);
    }

    public Sessao marcarCompareceu(String sessaoId) {
        Sessao s = get(sessaoId);
        s.marcarComparecimento();

        // ✅ Se for AVALIACAO e ainda não tem paciente, converte
        if (s.getTipo() == SessaoTipo.AVALIACAO && s.getPacienteId() == null) {
            Lead lead = leadRepository.findById(s.getLeadId())
                    .orElseThrow(() -> new RuntimeException("Lead não encontrado"));

            Paciente paciente = pacienteRepository.save(Paciente.fromLead(lead));

            // cria avaliação clínica aguardando
            avaliacaoRepository.save(Avaliacao.criarParaPaciente(paciente.getId()));

            // vincula sessão ao paciente
            s.setPaciente(paciente.getId());
            sessaoRepository.save(s);
        }

        return sessaoRepository.save(s);
    }

    private Sessao get(String id) {
        return sessaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));
    }
}
