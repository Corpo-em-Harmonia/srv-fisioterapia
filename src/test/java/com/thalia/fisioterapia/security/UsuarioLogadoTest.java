package com.thalia.fisioterapia.security;

import com.thalia.fisioterapia.domain.paciente.Paciente;
import com.thalia.fisioterapia.domain.sessao.PerfilUsuario;
import com.thalia.fisioterapia.infrastructure.repository.paciente.PacienteRepository;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UsuarioLogadoTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PacienteRepository pacienteRepository;

    private UsuarioLogado usuarioLogado;

    @BeforeEach
    void setUp() {
        usuarioLogado = new UsuarioLogado(usuarioRepository, pacienteRepository);
        Paciente ana = instanciarPaciente("pac-ana");
        lenient().when(pacienteRepository.findFirstByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void paciente_acessaOsProprioDados() {
        logar("ana@x.com", "PACIENTE");
        assertThatNoException().isThrownBy(() -> usuarioLogado.garantirAcessoAoPaciente("pac-ana"));
    }

    @Test
    void paciente_naoAcessaDadosDeOutroPaciente() {
        logar("ana@x.com", "PACIENTE");
        assertThatThrownBy(() -> usuarioLogado.garantirAcessoAoPaciente("pac-bruno"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void equipe_acessaQualquerPaciente() {
        logar("recepcao@x.com", "RECEPCIONISTA");
        assertThatNoException().isThrownBy(() -> usuarioLogado.garantirAcessoAoPaciente("pac-bruno"));
        assertThat(usuarioLogado.ehEquipe()).isTrue();
    }

    @Test
    void perfilDaAuditoria_usaOPerfilDeMaiorAlcance() {
        logar("fisio@x.com", "FISIOTERAPEUTA", "RECEPCIONISTA");
        assertThat(usuarioLogado.perfilAuditoria()).isEqualTo(PerfilUsuario.RECEPCAO);

        logar("fisio@x.com", "FISIOTERAPEUTA");
        assertThat(usuarioLogado.perfilAuditoria()).isEqualTo(PerfilUsuario.FISIO);
    }

    @Test
    void semUsuario_auditoriaRegistraSistema() {
        assertThat(usuarioLogado.usuarioId()).isEqualTo("sistema");
    }

    private void logar(String email, String... roles) {
        var authorities = Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
    }

    private Paciente instanciarPaciente(String id) {
        try {
            var construtor = Paciente.class.getDeclaredConstructor();
            construtor.setAccessible(true);
            Paciente p = construtor.newInstance();
            ReflectionTestUtils.setField(p, "id", id);
            return p;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
