package com.thalia.fisioterapia.infrastructure.repository.usuario;

import com.thalia.fisioterapia.domain.usuario.Role;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = "{ '$or': [ { 'roles': ?0 }, { 'role': ?0 } ] }", exists = true)
    boolean existsByRole(Role role);

    @Query(value = "{ '$or': [ { 'roles': ?0 }, { 'role': ?0 } ] }")
    Page<Usuario> findByRole(Role role, Pageable pageable);

    @Query("{ '$or': [ { 'roles': ?0 }, { 'role': ?0 } ] }")
    List<Usuario> findByRoleOrderByNomeAsc(Role role);
}
