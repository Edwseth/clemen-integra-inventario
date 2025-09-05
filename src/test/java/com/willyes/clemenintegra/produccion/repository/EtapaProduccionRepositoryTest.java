package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EtapaProduccionRepositoryTest {

    @Autowired
    private OrdenProduccionRepository ordenRepository;

    @Autowired
    private EtapaProduccionRepository etapaRepository;

    @Test
    void permiteNombresRepetidosEnDistintasOrdenes() {
        OrdenProduccion orden1 = crearOrden("OP-1");
        OrdenProduccion orden2 = crearOrden("OP-2");
        ordenRepository.save(orden1);
        ordenRepository.save(orden2);

        EtapaProduccion e1 = EtapaProduccion.builder()
                .nombre("Mezcla")
                .secuencia(1)
                .ordenProduccion(orden1)
                .build();
        EtapaProduccion e2 = EtapaProduccion.builder()
                .nombre("Mezcla")
                .secuencia(1)
                .ordenProduccion(orden2)
                .build();

        etapaRepository.save(e1);
        etapaRepository.save(e2);

        assertThat(etapaRepository.count()).isEqualTo(2);
    }

    private OrdenProduccion crearOrden(String codigo) {
        return OrdenProduccion.builder()
                .codigoOrden(codigo)
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(BigDecimal.ONE)
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.CREADA)
                .build();
    }
}
