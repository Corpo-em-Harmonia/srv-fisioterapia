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
                String ip = forwarded.split(",")[0].trim();
                if (ip.matches("^[\\d.]+$|^[\\da-fA-F:]+$")) { // IPv4 ou IPv6 básico
                    return ip;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
