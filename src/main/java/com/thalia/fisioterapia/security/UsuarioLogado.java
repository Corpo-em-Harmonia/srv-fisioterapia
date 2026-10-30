package com.thalia.fisioterapia.security;

import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.domain.sessao.PerfilUsuario;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quem está fazendo a requisição: usado na auditoria (antes tudo era gravado como "sistema")
 * e para restringir o paciente aos próprios dados.
 */
@Component
public class UsuarioLogado {

    private static final String SISTEMA = "sistema";
    private static final Set<String> PERFIS_EQUIPE = Set.of("ROLE_ADMIN", "ROLE_RECEPCIONISTA", "ROLE_FISIOTERAPEUTA");

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    public UsuarioLogado(UsuarioRepository usuarioRepository, PacienteRepository pacienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
    }

    public Optional<String> email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) return Optional.empty();
        return Optional.of(auth.getName());
    }

    private Set<String> authorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    public boolean ehEquipe() {
        return authorities().stream().anyMatch(PERFIS_EQUIPE::contains);
    }

    /** ID do usuário para a auditoria; "sistema" quando não há usuário (ex.: jobs, testes). */
    public String usuarioId() {
        return email().flatMap(usuarioRepository::findByEmail).map(Usuario::getId).orElse(SISTEMA);
    }

    /** Perfil gravado na auditoria. Quem tem mais de um perfil é registrado pelo de maior alcance. */
    public PerfilUsuario perfilAuditoria() {
        Set<String> roles = authorities();
        if (roles.contains("ROLE_ADMIN")) return PerfilUsuario.ADMIN;
        if (roles.contains("ROLE_RECEPCIONISTA")) return PerfilUsuario.RECEPCAO;
        if (roles.contains("ROLE_FISIOTERAPEUTA")) return PerfilUsuario.FISIO;
        return PerfilUsuario.ADMIN;
    }

    /** Paciente vinculado ao usuário logado (o cadastro do paciente usa o mesmo e-mail da conta). */
    public Optional<String> pacienteId() {
        return email().flatMap(pacienteRepository::findFirstByEmailIgnoreCase).map(Paciente::getId);
    }

    /** Equipe acessa qualquer paciente; o paciente, só o próprio cadastro. */
    public void garantirAcessoAoPaciente(String pacienteId) {
        if (ehEquipe()) return;
        boolean proprio = pacienteId != null && pacienteId().map(pacienteId::equals).orElse(false);
        if (!proprio) {
            throw new AccessDeniedException("Você só pode acessar os seus próprios dados.");
        }
    }
}
