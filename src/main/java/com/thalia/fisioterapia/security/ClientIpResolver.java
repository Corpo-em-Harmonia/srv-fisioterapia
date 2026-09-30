package com.thalia.fisioterapia.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private final boolean trustProxy;

    public ClientIpResolver(@Value("${app.security.trust-proxy:false}") boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public String resolve(HttpServletRequest request) {
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Só existe UM proxy reverso confiável na frente (Render). Um cliente pode mandar
                // qualquer coisa como primeiro valor de X-Forwarded-For (ex.: "1.2.3.4, <ip real>"),
                // mas não controla o que ESSE proxy anexa por último — por isso pegamos o último
                // valor da lista, não o primeiro, pra não permitir spoofar o IP usado no rate limit.
                String[] partes = forwarded.split(",");
                String ip = partes[partes.length - 1].trim();
                if (ip.matches("^[\\d.]+$|^[\\da-fA-F:]+$")) { // IPv4 ou IPv6 básico
                    return ip;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
