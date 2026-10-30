package com.thalia.fisioterapia.domain.avaliacao;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "avaliacoes")
@CompoundIndex(name = "pacienteId_criadaEm", def = "{ 'pacienteId': 1, 'criadaEm': -1 }")
@CompoundIndex(name = "status_finalizadaEm", def = "{ 'status': 1, 'finalizadaEm': -1 }")
@Getter
public class Avaliacao {

    @Id
    private String id;

    private String pacienteId;

    private AvaliacaoStatus status;
    private LocalDateTime criadaEm;
    private LocalDateTime finalizadaEm;

    // Auditoria do prontuário: quem finalizou e quem fez a última edição (ID do usuário)
    private String finalizadaPor;
    private String atualizadaPor;
    private LocalDateTime atualizadaEm;

    private String medico;
    private String hda;
    private String hpp;
    private String diagnostico;
    private String testesRealizados;
    private String goniometria;
    private String condutaTerapeutica;
    private String prognostico;
    private String desfecho;
    private String comodidade;
    private String medicamentos;
    private String cirurgia;

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
        finalizar(ficha, null);
    }

    public void finalizar(FichaClinica ficha, String usuarioId) {
        if (this.status != AvaliacaoStatus.EM_ATENDIMENTO) {
            throw new IllegalStateException("Atendimento não iniciado");
        }
        aplicarFicha(ficha);
        this.status = AvaliacaoStatus.FINALIZADA;
        this.finalizadaEm = LocalDateTime.now();
        this.finalizadaPor = usuarioId;
    }

    public void atualizar(FichaClinica ficha) {
        atualizar(ficha, null);
    }

    /** Editar não mexe mais em finalizadaEm — antes a edição sobrescrevia a data real da avaliação. */
    public void atualizar(FichaClinica ficha, String usuarioId) {
        if (this.status == AvaliacaoStatus.AGUARDANDO) {
            throw new IllegalStateException("Avaliação ainda não foi realizada");
        }
        aplicarFicha(ficha);
        this.atualizadaEm = LocalDateTime.now();
        this.atualizadaPor = usuarioId;
    }

    public FichaClinica getFichaClinica() {
        return new FichaClinica(
                medico,
                hda,
                hpp,
                diagnostico,
                testesRealizados,
                goniometria,
                condutaTerapeutica,
                prognostico,
                desfecho,
                comodidade,
                medicamentos,
                cirurgia
        );
    }

    private void aplicarFicha(FichaClinica ficha) {
        this.medico             = ficha.medico();
        this.hda                = ficha.hda();
        this.hpp                = ficha.hpp();
        this.diagnostico        = ficha.diagnostico();
        this.testesRealizados   = ficha.testesRealizados();
        this.goniometria        = ficha.goniometria();
        this.condutaTerapeutica = ficha.condutaTerapeutica();
        this.prognostico        = ficha.prognostico();
        this.desfecho           = ficha.desfecho();
        this.comodidade         = ficha.comodidade();
        this.medicamentos       = ficha.medicamentos();
        this.cirurgia           = ficha.cirurgia();
    }
}
