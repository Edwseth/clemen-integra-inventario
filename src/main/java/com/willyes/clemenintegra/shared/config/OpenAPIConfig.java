package com.willyes.clemenintegra.shared.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clemen-Integra ERP – Backend Modular")
                        .version("1.0.0")
                        .description("Documentación de los servicios REST para los módulos de Clemen-Integra")
                        .contact(new Contact()
                                .name("Will Yes Solutions")
                                .email("soporte@willyessolutions.com")))
                .servers(List.of(new Server().url("http://localhost:8080")));
    }
}

