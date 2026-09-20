package com.thalia.fisioterapia.infrastructure.repository.usuario;

import com.thalia.fisioterapia.domain.usuario.Role;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<Usuario> findAllByOrderByCriadoEmDesc();
}
