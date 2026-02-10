package com.willyes.clemenintegra.produccion.validators;

import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduccionEtapasLockValidatorTest {

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private CierreProduccionRepository cierreProduccionRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private EtapaProduccionRepository etapaProduccionRepository;

    private ProduccionEtapasLockValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProduccionEtapasLockValidator(
                ordenProduccionRepository,
                cierreProduccionRepository,
                movimientoInventarioRepository,
                etapaProduccionRepository
        );
    }

    @Test
    @DisplayName("Test 1: OP EN_PROCESO bloquea edición de etapas")
    void opEnProceso_bloquea() {
        long ordenId = 1L;
        mockOrden(ordenId, EstadoProduccion.EN_PROCESO);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> validator.assertEtapasEditables(ordenId));

        assertEquals(ApiErrorCode.OP_ETAPAS_BLOQUEADAS, ex.getCode());
    }

    @Test
    @DisplayName("Test 2: OP CREADA con cierre_produccion bloquea edición")
    void opCreadaConCierre_bloquea() {
        long ordenId = 2L;
        mockOrden(ordenId, EstadoProduccion.CREADA);
        when(cierreProduccionRepository.existsByOrdenProduccionId(ordenId)).thenReturn(true);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> validator.assertEtapasEditables(ordenId));

        assertEquals(ApiErrorCode.OP_ETAPAS_BLOQUEADAS, ex.getCode());
    }

    @Test
    @DisplayName("Test 3: OP CREADA con movimientos inventario bloquea edición")
    void opCreadaConMovimientos_bloquea() {
        long ordenId = 3L;
        mockOrden(ordenId, EstadoProduccion.CREADA);
        when(movimientoInventarioRepository.existsByOrdenProduccionId(ordenId)).thenReturn(true);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> validator.assertEtapasEditables(ordenId));

        assertEquals(ApiErrorCode.OP_ETAPAS_BLOQUEADAS, ex.getCode());
    }

    @Test
    @DisplayName("Test 4: OP CREADA con etapa.fecha_inicio no null bloquea edición")
    void opCreadaConEtapaIniciada_bloquea() {
        long ordenId = 4L;
        mockOrden(ordenId, EstadoProduccion.CREADA);
        when(etapaProduccionRepository.existsByOrdenProduccionIdAndFechaInicioIsNotNull(ordenId)).thenReturn(true);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> validator.assertEtapasEditables(ordenId));

        assertEquals(ApiErrorCode.OP_ETAPAS_BLOQUEADAS, ex.getCode());
    }

    @Test
    @DisplayName("Test 5: OP CREADA sin evidencias permite mutación")
    void opCreadaSinEvidencias_permite() {
        long ordenId = 5L;
        mockOrden(ordenId, EstadoProduccion.CREADA);

        assertDoesNotThrow(() -> validator.assertEtapasEditables(ordenId));
    }

    private void mockOrden(Long ordenId, EstadoProduccion estado) {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(ordenId);
        orden.setEstado(estado);

        when(ordenProduccionRepository.findById(ordenId)).thenReturn(Optional.of(orden));
        lenient().when(cierreProduccionRepository.existsByOrdenProduccionId(ordenId)).thenReturn(false);
        lenient().when(movimientoInventarioRepository.existsByOrdenProduccionId(ordenId)).thenReturn(false);
        lenient().when(etapaProduccionRepository.existsByOrdenProduccionIdAndFechaInicioIsNotNull(ordenId)).thenReturn(false);
    }
}
