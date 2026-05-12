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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
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
            boolean senhaGerada = adminPassword == null || adminPassword.isBlank();
            String senha = senhaGerada ? gerarSenhaAleatoria() : adminPassword;

            var admin = new Usuario("Admin", adminEmail, passwordEncoder.encode(senha), Role.ADMIN);
            usuarioRepository.save(admin);

            if (senhaGerada) {
                salvarCredenciaisEmArquivo(senha);
            } else {
                log.warn("Admin criado — email={} | senha via variável de ambiente", adminEmail);
            }
        }
    }

    private void salvarCredenciaisEmArquivo(String senha) {
        Path arquivo = Path.of("admin-credentials.txt");
        String conteudo = """
                ============================================================
                 CREDENCIAIS DO ADMINISTRADOR — DELETE APÓS O PRIMEIRO LOGIN
                ============================================================
                Email   : %s
                Senha   : %s
                Gerado  : %s
                ============================================================
                Acesse o sistema, troque a senha e delete este arquivo.
                """.formatted(adminEmail, senha, Instant.now());
        try {
            Files.writeString(arquivo, conteudo);
            log.warn("=============================================================");
            log.warn("  Admin criado — credenciais salvas em: {}", arquivo.toAbsolutePath());
            log.warn("  DELETE o arquivo após o primeiro login e troque a senha.");
            log.warn("=============================================================");
        } catch (IOException e) {
            log.error("Não foi possível salvar credenciais em arquivo. " +
                      "Defina ADMIN_PASSWORD e ADMIN_EMAIL e reinicie com banco limpo.", e);
        }
    }

    private String gerarSenhaAleatoria() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
