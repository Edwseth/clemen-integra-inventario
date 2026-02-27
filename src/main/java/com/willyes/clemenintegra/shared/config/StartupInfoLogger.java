package com.willyes.clemenintegra.shared.config;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupInfoLogger implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment environment;

    public StartupInfoLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String profiles = Arrays.stream(environment.getActiveProfiles())
                .collect(Collectors.joining(", "));
        if (profiles.isBlank()) {
            profiles = "default";
        }

        String port = environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8080"));
        String address = environment.getProperty("server.address", "0.0.0.0");
        String contextPath = environment.getProperty("server.servlet.context-path", "/");
        String allowedOrigins = environment.getProperty("app.cors.allowed-origins", "");
        String datasourceUrl = environment.getProperty("spring.datasource.url", "<not-configured>");

        log.info("Runtime profile(s): {}", profiles);
        log.info("Server address: {}:{}", address, port);
        log.info("Context path: {}", contextPath);
        log.info("Datasource URL: {}", datasourceUrl);
        log.info("CORS allowed origins: {}", allowedOrigins.isBlank() ? "<none>" : allowedOrigins);
    }
}
