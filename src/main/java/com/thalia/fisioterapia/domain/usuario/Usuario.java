package com.thalia.fisioterapia.domain.usuario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    private Set<Role> roles = new LinkedHashSet<>();

    private boolean ativo = true;

    private LocalDateTime criadoEm;

    protected Usuario() {}

    public Usuario(String nome, String email, String senha, Role role) {
        this(nome, email, senha, Set.of(role));
    }

    public Usuario(String nome, String email, String senha, Set<Role> roles) {
        this.nome     = nome;
        this.email    = email;
        this.senha    = senha;
        this.roles    = new LinkedHashSet<>(roles);
        this.role     = this.roles.iterator().next();
        this.criadoEm = LocalDateTime.now();
    }

    public Set<Role> getRoles() {
        if (roles == null || roles.isEmpty()) {
            return role == null ? Set.of() : Set.of(role);
        }
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = new LinkedHashSet<>(roles);
        this.role = this.roles.iterator().next();
    }
}
