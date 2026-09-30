package com.thalia.fisioterapia.config;

import com.thalia.fisioterapia.security.JwtAuthFilter;
import com.thalia.fisioterapia.security.LeadRateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CorsConfigurationSource corsConfigurationSource,
                                           JwtAuthFilter jwtAuthFilter,
                                           LeadRateLimitFilter leadRateLimitFilter) throws Exception {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean requireHttps = environment.getProperty("app.security.require-https", Boolean.class, false);

        if (requireHttps) {
            // O Spring Security 7.1.1 removeu o pacote web.access.channel usado por
            // requiresChannel()/ChannelDecisionManager (a config ainda referencia a
            // classe, mas ela não existe mais em spring-security-web — NoClassDefFoundError
            // em runtime). Redireciona manualmente com base em request.isSecure(), que já
            // respeita X-Forwarded-Proto quando server.forward-headers-strategy=framework.
            http.addFilterBefore(new RequireHttpsFilter(), UsernamePasswordAuthenticationFilter.class);
        }

        http
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000));
                    headers.permissionsPolicyHeader(p -> p.policy("geolocation=(), camera=(), microphone=()"));
                    // CSP restritivo só em prod: em dev o Swagger UI precisa de scripts/estilos inline.
                    if (isProd) {
                        headers.contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
                    }
                })
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/api/auth/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/leads").permitAll();
                    if (!isProd) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }
                    auth.requestMatchers("/api/**").authenticated();
                    auth.anyRequest().denyAll();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpStatus.UNAUTHORIZED.value(), "Não autenticado"))
                )
                .addFilterBefore(leadRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // createDelegatingPasswordEncoder() registra "bcrypt" -> new BCryptPasswordEncoder() (força 10
        // por padrão); setDefaultPasswordEncoderForMatches só afeta o fallback usado para comparar
        // hashes legados sem prefixo {bcrypt} — não muda o que .encode() realmente usa. Substituímos
        // a entrada "bcrypt" do mapa para que o encode (admin bootstrap, criação de usuário, reset de
        // senha) use força 12 de verdade, como documentado.
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);
        String idForEncode = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(idForEncode, bcrypt);
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(idForEncode, encoders);
        encoder.setDefaultPasswordEncoderForMatches(bcrypt);
        return encoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String[] allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** Redireciona para HTTPS quando app.security.require-https=true (ver comentário em filterChain). */
    private static class RequireHttpsFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (!request.isSecure()) {
                String query = request.getQueryString();
                String url = "https://" + request.getServerName() + request.getRequestURI()
                        + (query != null ? "?" + query : "");
                response.sendRedirect(url);
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
