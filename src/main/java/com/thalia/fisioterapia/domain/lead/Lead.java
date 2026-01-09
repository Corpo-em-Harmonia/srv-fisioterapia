package com.thalia.fisioterapia.domain.lead;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
public class Lead {

    @Id
    private String id;
    private String nome;
    private String sobrenome;
    private String email;
    private String celular;
    private String motivo;
    private LeadStatus status;


    public Lead(String nome, String sobrenome, String email, String celular,String motivo) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.celular = celular;
        this.status = LeadStatus.NOVO;
        this.motivo = motivo;
    }

    public Lead(
            String id,
            String nome,
            String sobrenome,
            String email,
            String celular,
            LeadStatus status,
            String motivo
    ) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.celular = celular;
        this.status = status;
        this.motivo = motivo;
    }

    public void iniciarAvaliacao() {
        if (this.status != LeadStatus.NOVO) {
            throw new IllegalStateException("Lead não pode iniciar avaliação");
        }
        this.status = LeadStatus.AVALIACAO_INICIADA;
    }

    public void finalizarAvaliacao() {
        if (status != LeadStatus.AVALIACAO_INICIADA) {
            throw new IllegalStateException("Avaliação não iniciada");
        }
        this.status = LeadStatus.PACIENTE;
    }




    public void marcarComoConvertido() {
        this.status = LeadStatus.AVALIACAO_INICIADA;
    }
}
