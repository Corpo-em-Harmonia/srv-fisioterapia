package com.thalia.fisioterapia.web.controller;

import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import com.thalia.fisioterapia.security.JwtService;
import com.thalia.fisioterapia.security.LoginAttemptService;
import com.thalia.fisioterapia.web.dto.auth.LoginRequest;
import com.thalia.fisioterapia.web.dto.auth.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final UsuarioRepository     usuarioRepository;
    private final LoginAttemptService   loginAttemptService;

    @Value("${app.security.trust-proxy:false}")
    private boolean trustProxy;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String ip = resolverIp(httpRequest);

        if (loginAttemptService.estaBloqueado(ip)) {
            log.warn("Login bloqueado por rate limit: ip={} email={}", ip, request.email());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.replace("ROLE_", ""))
                    .orElse("RECEPCIONISTA");

            String nome = usuarioRepository.findByEmail(request.email())
                    .map(Usuario::getNome)
                    .orElse(request.email());

            loginAttemptService.registrarSucesso(ip);
            String token = jwtService.generateToken(request.email(), role, nome);
            log.info("Login realizado: email={} role={}", request.email(), role);
            return ResponseEntity.ok(new LoginResponse(token, role.toLowerCase(), nome));

        } catch (DisabledException e) {
            loginAttemptService.registrarFalha(ip);
            log.warn("Usuário inativo tentou login: email={}", request.email());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (BadCredentialsException e) {
            loginAttemptService.registrarFalha(ip);
            log.warn("Credenciais inválidas: ip={} email={}", ip, request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private String resolverIp(HttpServletRequest request) {
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String ip = forwarded.split(",")[0].trim();
                if (ip.matches("^[\\d.]+$|^[\\da-fA-F:]+$")) { // IPv4 ou IPv6 básico
                    return ip;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
