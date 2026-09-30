package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.ConflictException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import com.thalia.fisioterapia.domain.usuario.Role;
import com.thalia.fisioterapia.web.dto.usuario.AtualizarUsuarioRequest;
import com.thalia.fisioterapia.web.dto.usuario.CriarUsuarioRequest;
import com.thalia.fisioterapia.web.dto.usuario.ResetSenhaRequest;
import com.thalia.fisioterapia.web.dto.usuario.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;

    /** ADMIN vê todos os usuários; qualquer outro perfil autorizado (recepção) vê só pacientes. */
    public Page<UsuarioResponse> listar(Pageable pageable) {
        Page<Usuario> usuarios = chamadorTemPapel(Role.ADMIN)
                ? usuarioRepository.findAll(pageable)
                : usuarioRepository.findByRole(Role.PACIENTE, pageable);
        return usuarios.map(this::toResponse);
    }

    public List<UsuarioResponse> listarFisioterapeutas() {
        return usuarioRepository.findByRoleOrderByNomeAsc(Role.FISIOTERAPEUTA).stream()
                .filter(Usuario::isAtivo)
                .map(this::toResponse)
                .toList();
    }

    public UsuarioResponse criar(CriarUsuarioRequest request) {
        exigirPermissaoParaCriar(request.role());
        String email = normalizarEmail(request.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }

        var usuario = new Usuario(
                request.nome(),
                email,
                passwordEncoder.encode(request.senha()),
                request.role()
        );

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuário criado: email={} role={}", saved.getEmail(), saved.getRole());
        return toResponse(saved);
    }

    public UsuarioResponse atualizar(String id, AtualizarUsuarioRequest request) {
        exigirPapel(Role.ADMIN);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        String email = normalizarEmail(request.email());

        if (!usuario.getEmail().equalsIgnoreCase(email) && usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado por outro usuário");
        }

        usuario.setNome(request.nome());
        usuario.setEmail(email);
        usuario.setRole(request.role());
        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuário [{}] atualizado por admin", saved.getEmail());
        return toResponse(saved);
    }

    public void criarParaPacienteSeNaoExistir(String nome, String sobrenome, String email, String telefone) {
        if (email == null || email.isBlank() || email.contains(".temp")) return;
        String emailNormalizado = normalizarEmail(email);
        if (usuarioRepository.existsByEmail(emailNormalizado)) return;

        String nomeCompleto = (nome + (sobrenome != null && !sobrenome.isBlank() ? " " + sobrenome : "")).trim();
        String senhaInicial = gerarSenhaAleatoria();

        var usuario = new Usuario(nomeCompleto, emailNormalizado, passwordEncoder.encode(senhaInicial), Role.PACIENTE);
        usuarioRepository.save(usuario);
        log.info("Usuário criado para paciente: email={} — admin deve definir senha via reset-senha", emailNormalizado);
    }

    /**
     * Normaliza pra minúsculas antes de gravar/checar — o índice único do Mongo e as
     * queries derivadas (existsByEmail) são case-sensitive, então sem isso
     * "joao@x.com" e "Joao@X.com" seriam tratados como contas diferentes.
     */
    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String gerarSenhaAleatoria() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void resetSenha(String id, ResetSenhaRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!chamadorTemPapel(Role.ADMIN) && usuario.getRole() != Role.PACIENTE) {
            throw new AccessDeniedException("Você só pode redefinir a senha de pacientes.");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
        log.info("Senha do usuário [{}] redefinida por {}", usuario.getEmail(), chamadorAtual());
    }

    /**
     * Regra de negócio (não só de rota): ADMIN cria qualquer perfil; RECEPCIONISTA só cria PACIENTE.
     * Fica aqui para que um recepcionista não consiga criar ADMIN enviando role: "ADMIN".
     */
    private void exigirPermissaoParaCriar(Role roleSolicitado) {
        if (chamadorTemPapel(Role.ADMIN)) {
            return;
        }
        if (chamadorTemPapel(Role.RECEPCIONISTA) && roleSolicitado == Role.PACIENTE) {
            return;
        }
        log.warn("Criação de usuário negada: chamador={} role solicitado={}", chamadorAtual(), roleSolicitado);
        throw new AccessDeniedException("Você só pode cadastrar usuários com o perfil Paciente.");
    }

    /**
     * Defesa em profundidade: mesmo com @PreAuthorize("hasRole('ADMIN')") no controller,
     * garante no service que só ADMIN passa por aqui — atualizar()/alternarStatus() podem
     * promover qualquer usuário a ADMIN, então não devem depender só da anotação do controller
     * (mesmo padrão que já protege criar()/resetSenha()).
     */
    private void exigirPapel(Role role) {
        if (!chamadorTemPapel(role)) {
            throw new AccessDeniedException("Ação restrita ao perfil " + role.name() + ".");
        }
    }

    private boolean chamadorTemPapel(Role role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
    }

    private String chamadorAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? String.valueOf(auth.getName()) : "desconhecido";
    }

    public UsuarioResponse alternarStatus(String id) {
        exigirPapel(Role.ADMIN);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        usuario.setAtivo(!usuario.isAtivo());
        Usuario saved = usuarioRepository.save(usuario);
        log.info("Status do usuário [{}] alterado para ativo={}", saved.getEmail(), saved.isAtivo());
        return toResponse(saved);
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getRole().name().toLowerCase(),
                u.isAtivo(),
                u.getCriadoEm()
        );
    }
}
