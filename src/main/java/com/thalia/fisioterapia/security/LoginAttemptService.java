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
    private static final int  LIMITE_CACHE     = 10_000;

    private record Tentativas(int count, Instant primeiraTentativa) {}

    private final ConcurrentHashMap<String, Tentativas> cache = new ConcurrentHashMap<>();

    // IPs que falham abaixo do limite de bloqueio nunca eram removidos do cache — cresce sem
    // limite com tráfego distribuído (credential stuffing com IPs rotativos, ou só churn normal
    // ao longo do tempo). Faz uma varredura de expirados quando o cache cresce demais, mesmo
    // padrão já usado pelo LeadRateLimitFilter.
    private void evictExpirados() {
        if (cache.size() <= LIMITE_CACHE) return;
        Instant agora = Instant.now();
        cache.values().removeIf(t -> agora.isAfter(t.primeiraTentativa().plusSeconds(BLOQUEIO_SEGUNDOS)));
    }

    public boolean estaBloqueado(String ip) {
        evictExpirados();
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
        evictExpirados();
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
