package com.willyes.clemenintegra.produccion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

@Component
@Slf4j
public class ProduccionEndpointLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final String MOVIMIENTOS_ETAPA_PATH = "/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/movimientos";
    @Qualifier("requestMappingHandlerMapping")
    private final RequestMappingHandlerMapping handlerMapping;

    public ProduccionEndpointLogger(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping
    ) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Set<RequestMappingInfo> coincidencias = handlerMapping.getHandlerMethods()
                .keySet()
                .stream()
                .filter(info -> info.getPatternValues() != null
                        && info.getPatternValues().stream()
                        .anyMatch(pattern -> pattern.equals(MOVIMIENTOS_ETAPA_PATH)))
                .collect(java.util.stream.Collectors.toSet());

        if (coincidencias.isEmpty()) {
            log.warn("Producción: mapping NO encontrado para {}", MOVIMIENTOS_ETAPA_PATH);
        } else {
            log.info("Producción: mapping activo para {} ({} coincidencias)", MOVIMIENTOS_ETAPA_PATH, coincidencias.size());
        }
    }
}
