package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import com.thalia.fisioterapia.security.ClientIpResolver;
import com.thalia.fisioterapia.security.JwtService;
import com.thalia.fisioterapia.security.LoginAttemptService;
import com.thalia.fisioterapia.web.dto.auth.LoginRequest;
import com.thalia.fisioterapia.web.dto.auth.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final UsuarioRepository     usuarioRepository;
    private final LoginAttemptService   loginAttemptService;
    private final ClientIpResolver      clientIpResolver;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String ip = clientIpResolver.resolve(httpRequest);

        if (loginAttemptService.estaBloqueado(ip)) {
            log.warn("Login bloqueado por rate limit: ip={} email={}", ip, request.email());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

                List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.replace("ROLE_", ""))
                    .toList();
                String role = roles.stream().findFirst().orElse("RECEPCIONISTA");

            String nome = usuarioRepository.findByEmail(request.email())
                    .map(Usuario::getNome)
                    .orElse(request.email());

            loginAttemptService.registrarSucesso(ip);
            String token = jwtService.generateToken(request.email(), roles, nome);
            log.info("Login realizado: email={} roles={}", request.email(), roles);
                return ResponseEntity.ok(new LoginResponse(token, role.toLowerCase(),
                    roles.stream().map(String::toLowerCase).toList(), nome));

        } catch (DisabledException e) {
            // Mesmo status/resposta de credenciais inválidas — devolver 403 aqui permitiria
            // enumerar contas desativadas (ex-funcionários, pacientes suspensos) só observando
            // o código HTTP, sem precisar acertar a senha.
            loginAttemptService.registrarFalha(ip);
            log.warn("Usuário inativo tentou login: email={}", request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (BadCredentialsException e) {
            loginAttemptService.registrarFalha(ip);
            log.warn("Credenciais inválidas: ip={} email={}", ip, request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (AuthenticationException e) {
            // Qualquer outra falha de autenticação (ex.: Mongo momentaneamente indisponível
            // durante o login) — 401 em vez de cair no handler genérico de 500.
            loginAttemptService.registrarFalha(ip);
            log.warn("Falha de autenticação inesperada: ip={} email={} tipo={}", ip, request.email(), e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
