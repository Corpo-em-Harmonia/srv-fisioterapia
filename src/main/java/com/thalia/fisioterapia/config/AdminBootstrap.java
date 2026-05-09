package com.thalia.fisioterapia.config;

import com.thalia.fisioterapia.domain.usuario.Role;
import com.thalia.fisioterapia.domain.usuario.Usuario;
import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;

    @Value("${auth.admin.email:admin@clinica.com}")
    private String adminEmail;

    @Value("${auth.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!usuarioRepository.existsByRole(Role.ADMIN)) {
            String senha = adminPassword != null && !adminPassword.isBlank()
                    ? adminPassword
                    : gerarSenhaAleatoria();

            var admin = new Usuario("Admin", adminEmail, passwordEncoder.encode(senha), Role.ADMIN);
            usuarioRepository.save(admin);

            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("==========================================================");
                log.warn("  ADMIN CRIADO COM SENHA GERADA AUTOMATICAMENTE");
                log.warn("  Email: {}", adminEmail);
                log.warn("  Senha: {}", senha);
                log.warn("  Defina ADMIN_PASSWORD no ambiente para controlar a senha.");
                log.warn("==========================================================");
            } else {
                log.warn("Admin criado — email={} | Senha definida via variável de ambiente", adminEmail);
            }
        }
    }

    private String gerarSenhaAleatoria() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
