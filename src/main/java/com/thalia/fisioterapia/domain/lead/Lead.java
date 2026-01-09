package com.thalia.fisioterapia.domain.lead;


import lombok.Data;

@Data
public class Lead {

    private final LeadId id;
    private String nome;
    private String sobrenome;
    private String email;
    private String celular;

    private LeadStatus status;


    public Lead(String nome, String sobrenome, String email, String celular) {
        this.id = LeadId.generate();
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.celular = celular;
        this.status = LeadStatus.NOVO;
    }

    public Lead(
            LeadId id,
            String nome,
            String sobrenome,
            String email,
            String celular,
            LeadStatus status
    ) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.celular = celular;
        this.status = status;
    }





    public void marcarComoConvertido() {
        this.status = LeadStatus.CONVERTIDO;
    }
}
