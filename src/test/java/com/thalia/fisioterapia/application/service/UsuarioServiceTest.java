package com.thalia.fisioterapia.application.service;

import com.thalia.fisioterapia.application.exception.ConflictException;
import com.thalia.fisioterapia.domain.usuario.Role;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import com.thalia.fisioterapia.web.dto.usuario.CriarUsuarioRequest;
import com.thalia.fisioterapia.web.dto.usuario.ResetSenhaRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private UsuarioRepository repository;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("hash");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new UsuarioService(repository, encoder);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Role role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("quem@clinica.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private CriarUsuarioRequest pedido(Role role) {
        return new CriarUsuarioRequest("Fulano", "fulano@clinica.com", "senha1234", role);
    }

    @Test
    void recepcionistaNaoPodeCriarAdmin() {
        autenticarComo(Role.RECEPCIONISTA);

        assertThatThrownBy(() -> service.criar(pedido(Role.ADMIN)))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void recepcionistaPodeCriarPaciente() {
        autenticarComo(Role.RECEPCIONISTA);

        var resp = service.criar(pedido(Role.PACIENTE));

        assertThat(resp.role()).isEqualTo("paciente");
    }

    @Test
    void adminPodeCriarQualquerPerfil() {
        autenticarComo(Role.ADMIN);

        assertThat(service.criar(pedido(Role.ADMIN)).role()).isEqualTo("admin");
    }

    @Test
    void fisioterapeutaNaoPodeCriarNemPaciente() {
        autenticarComo(Role.FISIOTERAPEUTA);

        assertThatThrownBy(() -> service.criar(pedido(Role.PACIENTE)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void semAutenticacaoNaoPodeCriar() {
        assertThatThrownBy(() -> service.criar(pedido(Role.PACIENTE)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void emailDuplicadoLancaConflito() {
        autenticarComo(Role.ADMIN);
        when(repository.existsByEmail("fulano@clinica.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(pedido(Role.PACIENTE)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já cadastrado");
    }

    @Test
    void recepcionistaListaApenasPacientes() {
        autenticarComo(Role.RECEPCIONISTA);
        when(repository.findAllByRoleOrderByCriadoEmDesc(Role.PACIENTE)).thenReturn(List.of());

        service.listar();

        verify(repository).findAllByRoleOrderByCriadoEmDesc(Role.PACIENTE);
        verify(repository, never()).findAllByOrderByCriadoEmDesc();
    }

    @Test
    void adminListaTodos() {
        autenticarComo(Role.ADMIN);
        when(repository.findAllByOrderByCriadoEmDesc()).thenReturn(List.of());

        service.listar();

        verify(repository).findAllByOrderByCriadoEmDesc();
    }

    @Test
    void recepcionistaNaoRedefineSenhaDeQuemNaoEPaciente() {
        autenticarComo(Role.RECEPCIONISTA);
        when(repository.findById("1")).thenReturn(
                Optional.of(new Usuario("Fisio", "fisio@clinica.com", "hash", Role.FISIOTERAPEUTA)));

        assertThatThrownBy(() -> service.resetSenha("1", new ResetSenhaRequest("novaSenha123")))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void recepcionistaRedefineSenhaDePaciente() {
        autenticarComo(Role.RECEPCIONISTA);
        when(repository.findById("2")).thenReturn(
                Optional.of(new Usuario("Pac", "pac@clinica.com", "hash", Role.PACIENTE)));

        service.resetSenha("2", new ResetSenhaRequest("novaSenha123"));

        verify(repository).save(any(Usuario.class));
    }
}
