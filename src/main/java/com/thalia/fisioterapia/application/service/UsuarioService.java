package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ResourceNotFoundException;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import com.thalia.fisioterapia.web.dto.usuario.AtualizarUsuarioRequest;
import com.thalia.fisioterapia.web.dto.usuario.CriarUsuarioRequest;
import com.thalia.fisioterapia.web.dto.usuario.ResetSenhaRequest;
import com.thalia.fisioterapia.web.dto.usuario.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAllByOrderByCriadoEmDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UsuarioResponse criar(CriarUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado");
        }

        var usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha()),
                request.role()
        );

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuário criado: email={} role={}", saved.getEmail(), saved.getRole());
        return toResponse(saved);
    }

    public UsuarioResponse atualizar(String id, AtualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!usuario.getEmail().equalsIgnoreCase(request.email())
                && usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado por outro usuário");
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setRole(request.role());
        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuário [{}] atualizado por admin", saved.getEmail());
        return toResponse(saved);
    }

    public void resetSenha(String id, ResetSenhaRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
        log.info("Senha do usuário [{}] redefinida por admin", usuario.getEmail());
    }

    public UsuarioResponse alternarStatus(String id) {
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
