package com.thalia.fisioterapia.domain.avaliacao;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "avaliacoes")
@Data
public class Avaliacao {

    @Id
    private String id;

    private String leadId;
    private AvaliacaoStatus status;

    private String medico;
    private String hda;
    private String hpp;
    private String diagnostico;
    private String condutaTerapeutica;
    private String prognostico;

    protected Avaliacao() {
        // construtor para o Mongo
    }

    private Avaliacao(String leadId) {
        this.leadId = leadId;
        this.status = AvaliacaoStatus.EM_ANDAMENTO;
    }

    public static Avaliacao iniciar(String leadId) {
        return new Avaliacao(leadId);
    }

    public void finalizar(
            String medico,
            String hda,
            String hpp,
            String diagnostico,
            String condutaTerapeutica,
            String prognostico
    ) {
        if (this.status != AvaliacaoStatus.EM_ANDAMENTO) {
            throw new IllegalStateException("Avaliação não está em andamento");
        }

        this.medico = medico;
        this.hda = hda;
        this.hpp = hpp;
        this.diagnostico = diagnostico;
        this.condutaTerapeutica = condutaTerapeutica;
        this.prognostico = prognostico;
        this.status = AvaliacaoStatus.FINALIZADA;
    }

    // getters (ou Lombok @Getter)
}
