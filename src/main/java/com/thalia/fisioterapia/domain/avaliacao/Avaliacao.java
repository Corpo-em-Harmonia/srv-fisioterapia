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

    // ====== FICHA CLÍNICA ======
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


    private Avaliacao(){}

    private Avaliacao(String pacienteId){
        this.pacienteId = pacienteId;
        this.status = AvaliacaoStatus.AGUARDANDO;
        this.criadaEm = LocalDateTime.now();
    }

    // fábrica (criação controlada)
    public static Avaliacao criarParaPaciente(String pacienteId){
        return new Avaliacao(pacienteId);
    }

    // início do atendimento
    public void iniciar(){
        if(this.status != AvaliacaoStatus.AGUARDANDO){
            throw new IllegalStateException("Avaliação já iniciada ou finalizada");
        }
        this.status = AvaliacaoStatus.EM_ATENDIMENTO;
    }

    // finalização
    public void finalizar(FichaClinica ficha){

        if(this.status != AvaliacaoStatus.EM_ATENDIMENTO){
            throw new IllegalStateException("Atendimento não iniciado");
        }

        this.medico = ficha.medico();
        this.hda = ficha.hda();
        this.hpp = ficha.hpp();
        this.diagnostico = ficha.diagnostico();
        this.testesRealizados = ficha.testesRealizados();
        this.goniometria = ficha.goniometria();
        this.condutaTerapeutica = ficha.condutaTerapeutica();
        this.prognostico = ficha.prognostico();
        this.desfecho = ficha.desfecho();
        this.comodidade = ficha.comodidade();
        this.medicamentos = ficha.medicamentos();
        this.cirurgia = ficha.cirurgia();

        this.status = AvaliacaoStatus.FINALIZADA;
        this.finalizadaEm = LocalDateTime.now();
    }
}
