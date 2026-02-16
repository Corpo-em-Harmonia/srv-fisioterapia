package com.thalia.fisioterapia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // API em dev: sem CSRF (senão POST/PATCH/DELETE costumam dar 403)
                .csrf(csrf -> csrf.disable())

                // usa o CORS definido abaixo
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // libera tudo (dev). Se depois tiver login, a gente restringe
                .authorizeHttpRequests(auth -> auth
                        // preflight do navegador
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // libera API
                        .requestMatchers("/api/**").permitAll()

                        // restante (html/static)
                        .anyRequest().permitAll()
                )

                // se você não usa login httpBasic nem formLogin, pode deixar assim
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String[] allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();

        // ex: app.cors.allowed-origins=http://localhost:4200,http://localhost:3000
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // IMPORTANTE: inclui PATCH
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // libera todos headers (inclui Authorization, Content-Type, etc.)
        configuration.setAllowedHeaders(List.of("*"));

        // se você NÃO usa cookie/sessão, pode trocar pra false
        configuration.setAllowCredentials(true);

        // opcional: caso você queira ler algum header no front
        configuration.setExposedHeaders(List.of("Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
