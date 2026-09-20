package com.thalia.fisioterapia.domain.usuario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "usuarios")
@Getter
@Setter
public class Usuario {

    @Id
    private String id;

    private String nome;

    @Indexed(unique = true)
    private String email;

    private String senha;

    private Role role;

    private boolean ativo = true;

    private LocalDateTime criadoEm;

    protected Usuario() {}

    public Usuario(String nome, String email, String senha, Role role) {
        this.nome     = nome;
        this.email    = email;
        this.senha    = senha;
        this.role     = role;
        this.criadoEm = LocalDateTime.now();
    }
}
