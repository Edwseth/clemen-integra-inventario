package com.willyes.clemenintegra.inventario.config;

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
                        .title("Clemen-Integra ERP - API de Inventario")
                        .version("1.0.0")
                        .description("Documentación de los servicios REST para el módulo de inventarios")
                        .contact(new Contact()
                                .name("Will Yes Solutions")
                                .email("soporte@willyessolutions.com")))
                .servers(List.of(new Server().url("http://localhost:8080")));
    }
}

