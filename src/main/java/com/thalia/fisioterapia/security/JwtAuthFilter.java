package com.thalia.fisioterapia.security;

import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService        jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims;
            try {
                claims = jwtService.parseToken(token);
            } catch (Exception e) {
                filterChain.doFilter(request, response);
                return;
            }

                String email = claims.getSubject();
                Object rolesClaim = claims.get("roles");
                List<String> roles = rolesClaim instanceof List<?> list
                        ? list.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .toList()
                        : List.of();
                if (roles == null || roles.isEmpty()) {
                    String role = claims.get("role", String.class);
                    roles = role == null ? List.of() : List.of(role);
                }

                boolean ativo = usuarioRepository.findByEmail(email)
                        .map(Usuario::isAtivo)
                        .orElse(false);

                if (ativo) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            email, null,
                                roles.stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                    .toList()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
        }

        filterChain.doFilter(request, response);
    }
}
