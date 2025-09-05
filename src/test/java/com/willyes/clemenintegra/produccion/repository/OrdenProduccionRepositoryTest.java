package com.willyes.clemenintegra.produccion.repository;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrdenProduccionRepositoryTest {

    @Autowired
    private OrdenProduccionRepository repository;

    @Test
    void obtieneLoteConMayorConsecutivoParaPrefijo() {
        repository.save(crearOrden("OP-1", "20231101-001"));
        repository.save(crearOrden("OP-2", "20231101-003"));
        repository.save(crearOrden("OP-3", "20231101-002"));
        repository.save(crearOrden("OP-4", "20231102-001"));

        Optional<OrdenProduccion> resultado = repository
                .findTopByLoteProduccionStartingWithOrderByLoteProduccionDesc("20231101");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getLoteProduccion()).isEqualTo("20231101-003");
    }

    private OrdenProduccion crearOrden(String codigo, String lote) {
        return OrdenProduccion.builder()
                .codigoOrden(codigo)
                .loteProduccion(lote)
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(BigDecimal.ONE)
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .estado(EstadoProduccion.CREADA)
                .build();
    }
}
