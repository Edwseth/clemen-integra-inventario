package com.willyes.clemenintegra.inventario.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ClasificacionMovimientoInventarioTest {

    @Test
    void deberiaContenerClasificacionesDeRegularizacionTrazabilidad() {
        assertThatCode(() -> ClasificacionMovimientoInventario.valueOf("REGULARIZACION_TRAZABILIDAD"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ClasificacionMovimientoInventario.valueOf("REGULARIZACION_TRAZABILIDAD_PT"))
                .doesNotThrowAnyException();
    }
}
