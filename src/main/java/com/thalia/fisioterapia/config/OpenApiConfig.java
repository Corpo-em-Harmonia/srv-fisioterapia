package com.thalia.fisioterapia.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fisioterapiaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Fisioterapia")
                        .description("Documentacao dos endpoints do backend de fisioterapia")
                        .version("v1")
                        .contact(new Contact()
                                .name("Equipe Fisioterapia")
                                .email("suporte@fisioterapia.local"))
                        .license(new License()
                                .name("Uso interno")
                                .url("https://example.com/internal-license")));
    }
}