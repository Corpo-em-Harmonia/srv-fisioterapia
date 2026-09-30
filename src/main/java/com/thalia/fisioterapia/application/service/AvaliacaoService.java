package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.avaliacao.Avaliacao;
import com.thalia.fisioterapia.domain.avaliacao.AvaliacaoStatus;
import com.thalia.fisioterapia.domain.avaliacao.FichaClinica;
import com.thalia.fisioterapia.domain.sessao.SessaoStatus;
import com.thalia.fisioterapia.infrastructure.repository.avaliacao.AvaliacaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.sessao.SessaoRepository;
import com.thalia.fisioterapia.infrastructure.repository.lead.LeadRepository;
import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoDetalheResponse;
import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoHistoricoResponse;
import com.thalia.fisioterapia.web.dto.avaliacao.AvaliacaoPendenteResponse;
import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.web.dto.avaliacao.FinalizarAvaliacaoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository repository;
    private final SessaoRepository sessaoRepository;
    private final LeadRepository leadRepository;
    private final PacienteRepository pacienteRepository;

    public AvaliacaoService(
            AvaliacaoRepository repository,
            SessaoRepository sessaoRepository,
            LeadRepository leadRepository,
            PacienteRepository pacienteRepository
    ) {
        this.repository = repository;
        this.sessaoRepository = sessaoRepository;
        this.leadRepository = leadRepository;
        this.pacienteRepository = pacienteRepository;
    }

    public void iniciar(String avaliacaoId){

        Avaliacao avaliacao = repository.findById(avaliacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada"));

        avaliacao.iniciar();

        repository.save(avaliacao);
    }

    public void finalizar(FinalizarAvaliacaoRequest request){
        Avaliacao avaliacao = repository.findById(request.getAvaliacaoId())
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada"));
        avaliacao.finalizar(fichaFrom(request));
        repository.save(avaliacao);
    }

    public AvaliacaoDetalheResponse atualizar(String id, FinalizarAvaliacaoRequest request) {
        Avaliacao avaliacao = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada"));
        avaliacao.atualizar(fichaFrom(request));
        Avaliacao salva = repository.save(avaliacao);
        return toDetalheResponse(salva);
    }

    private FichaClinica fichaFrom(FinalizarAvaliacaoRequest r) {
        return new FichaClinica(
                r.getMedico(), r.getHda(), r.getHpp(), r.getDiagnostico(),
                r.getTestesRealizados(), r.getGoniometria(), r.getCondutaTerapeutica(),
                r.getPrognostico(), r.getDesfecho(), r.getComodidade(),
                r.getMedicamentos(), r.getCirurgia()
        );
    }

    private AvaliacaoDetalheResponse toDetalheResponse(Avaliacao av) {
        return new AvaliacaoDetalheResponse(
                av.getId(), av.getPacienteId(),
                av.getStatus() != null ? av.getStatus().name().toLowerCase() : null,
                av.getMedico(), av.getHda(), av.getHpp(), av.getDiagnostico(),
                av.getTestesRealizados(), av.getGoniometria(), av.getCondutaTerapeutica(),
                av.getPrognostico(), av.getDesfecho(), av.getComodidade(),
                av.getMedicamentos(), av.getCirurgia(),
                av.getCriadaEm() != null ? av.getCriadaEm().toString() : null,
                av.getFinalizadaEm() != null ? av.getFinalizadaEm().toString() : null
        );
    }

    public Page<AvaliacaoPendenteResponse> listarPendentes(Pageable pageable) {
        Page<com.thalia.fisioterapia.domain.sessao.Sessao> sessoes =
                sessaoRepository.findByStatus(SessaoStatus.AGUARDANDO_AVALIACAO, pageable);

        // Busca em lote (findAllById) em vez de uma consulta por sessao, pra nao repetir
        // o N+1 que deixava /api/sessoes lento com paginas grandes.
        var leadIds = sessoes.getContent().stream()
                .map(com.thalia.fisioterapia.domain.sessao.Sessao::getLeadId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        var pacienteIds = sessoes.getContent().stream()
                .map(com.thalia.fisioterapia.domain.sessao.Sessao::getPacienteId).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Lead> leadsPorId = toMapById(leadRepository.findAllById(leadIds), Lead::getId);
        Map<String, Paciente> pacientesPorId = toMapById(pacienteRepository.findAllById(pacienteIds), Paciente::getId);

        return sessoes.map(sessao -> {
            String nome = null;
            String telefone = null;
            String origem;

            if (sessao.getLeadId() != null) {
                origem = "LEAD";
                var lead = leadsPorId.get(sessao.getLeadId());
                if (lead != null) {
                    nome = lead.getNome();
                    telefone = lead.getTelefone();
                }
            } else {
                origem = "PACIENTE";
                var paciente = pacientesPorId.get(sessao.getPacienteId());
                if (paciente != null) {
                    nome = paciente.getNome();
                    telefone = paciente.getTelefone();
                }
            }

            return new AvaliacaoPendenteResponse(
                    sessao.getId(),
                    sessao.getLeadId(),
                    sessao.getPacienteId(),
                    nome,
                    telefone,
                    sessao.getDataHora().toString(),
                    sessao.getStatus().name().toLowerCase(),
                    origem
            );
        });
    }

    private <T> Map<String, T> toMapById(Iterable<T> itens, Function<T, String> idExtractor) {
        Map<String, T> mapa = new HashMap<>();
        itens.forEach(item -> mapa.put(idExtractor.apply(item), item));
        return mapa;
    }

    public AvaliacaoDetalheResponse getDetalhe(String id) {
        return toDetalheResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada")));
    }

    public AvaliacaoDetalheResponse getDetalheByPaciente(String pacienteId) {
        return toDetalheResponse(repository.findFirstByPacienteIdOrderByCriadaEmDesc(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada para o paciente")));
    }

    public Page<AvaliacaoHistoricoResponse> listarHistorico(Pageable pageable) {
        Page<Avaliacao> avaliacoes = repository.findByStatus(AvaliacaoStatus.FINALIZADA, pageable);

        var pacienteIds = avaliacoes.getContent().stream()
                .map(Avaliacao::getPacienteId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        Map<String, Paciente> pacientesPorId = toMapById(pacienteRepository.findAllById(pacienteIds), Paciente::getId);

        return avaliacoes.map(avaliacao -> {
                    String nomePaciente = "Paciente nao vinculado";
                    if (avaliacao.getPacienteId() != null && !avaliacao.getPacienteId().isBlank()) {
                        Paciente p = pacientesPorId.get(avaliacao.getPacienteId());
                        nomePaciente = p != null
                                ? p.getNome() + (p.getSobrenome() != null ? " " + p.getSobrenome() : "")
                                : "Paciente nao encontrado";
                    }

                    String resumo = avaliacao.getDesfecho();
                    if (resumo == null || resumo.isBlank()) {
                        resumo = avaliacao.getDiagnostico();
                    }
                    if (resumo == null || resumo.isBlank()) {
                        resumo = "Sem resumo";
                    }

                    return new AvaliacaoHistoricoResponse(
                            nomePaciente.trim(),
                            avaliacao.getFinalizadaEm() != null ? avaliacao.getFinalizadaEm().toString() : null,
                            resumo,
                            avaliacao.getId()
                    );
                });
    }
}
