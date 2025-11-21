package com.willyes.clemenintegra.produccion.config;

import com.willyes.clemenintegra.produccion.service.ReporteOrdenProduccionService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ReporteOrdenProduccionTestConfig {

    @Bean
    public ReporteOrdenProduccionService reporteOrdenProduccionService() {
        return Mockito.mock(ReporteOrdenProduccionService.class);
    }
}
