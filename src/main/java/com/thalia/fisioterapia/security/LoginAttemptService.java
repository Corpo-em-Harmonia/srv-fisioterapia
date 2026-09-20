package com.thalia.fisioterapia.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LoginAttemptService {

    private static final int  MAX_TENTATIVAS  = 5;
    private static final long BLOQUEIO_SEGUNDOS = 15 * 60L; // 15 minutos

    private record Tentativas(int count, Instant primeiraTentativa) {}

    private final ConcurrentHashMap<String, Tentativas> cache = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String ip) {
        Tentativas t = cache.get(ip);
        if (t == null) return false;

        if (t.count() < MAX_TENTATIVAS) return false;

        boolean bloqueioExpirou = Instant.now().isAfter(
                t.primeiraTentativa().plusSeconds(BLOQUEIO_SEGUNDOS));

        if (bloqueioExpirou) {
            cache.remove(ip);
            return false;
        }

        return true;
    }

    public void registrarFalha(String ip) {
        cache.compute(ip, (key, atual) -> {
            if (atual == null) {
                return new Tentativas(1, Instant.now());
            }
            // se o bloqueio anterior já expirou, reinicia contagem
            boolean expirou = Instant.now().isAfter(
                    atual.primeiraTentativa().plusSeconds(BLOQUEIO_SEGUNDOS));
            if (expirou) {
                return new Tentativas(1, Instant.now());
            }
            int novoCount = atual.count() + 1;
            if (novoCount == MAX_TENTATIVAS) {
                log.warn("IP bloqueado por excesso de tentativas de login: {}", ip);
            }
            return new Tentativas(novoCount, atual.primeiraTentativa());
        });
    }

    public void registrarSucesso(String ip) {
        cache.remove(ip);
    }
}
