package com.thalia.fisioterapia.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit por IP para o endpoint público POST /api/leads (evita spam/bots).
 * Janela fixa em memória — reseta no restart, como o LoginAttemptService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeadRateLimitFilter extends OncePerRequestFilter {

    private static final int  MAX_REQUISICOES = 10;
    private static final long JANELA_SEGUNDOS = 10 * 60L; // 10 minutos
    private static final int  LIMITE_CACHE    = 10_000;

    private record Janela(int count, Instant inicio) {}

    private final ClientIpResolver clientIpResolver;
    private final ConcurrentHashMap<String, Janela> cache = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/api/leads".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIpResolver.resolve(request);

        if (excedeuLimite(ip)) {
            log.warn("Rate limit excedido em POST /api/leads: ip={}", ip);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean excedeuLimite(String ip) {
        Instant agora = Instant.now();
        if (cache.size() > LIMITE_CACHE) {
            cache.values().removeIf(j -> expirou(j, agora));
        }

        Janela janela = cache.compute(ip, (key, atual) ->
                (atual == null || expirou(atual, agora))
                        ? new Janela(1, agora)
                        : new Janela(atual.count() + 1, atual.inicio()));

        return janela.count() > MAX_REQUISICOES;
    }

    private boolean expirou(Janela janela, Instant agora) {
        return agora.isAfter(janela.inicio().plusSeconds(JANELA_SEGUNDOS));
    }
}
