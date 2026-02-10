package com.willyes.clemenintegra.produccion.validators;

import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProduccionEtapasLockValidator {

    private static final String MENSAJE_BLOQUEO = "No se permite modificar etapas: la orden ya inició o tiene registros asociados.";

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final CierreProduccionRepository cierreProduccionRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final EtapaProduccionRepository etapaProduccionRepository;

    public void assertEtapasEditables(Long ordenId) {
        OrdenProduccion orden = ordenProduccionRepository.findById(ordenId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Orden de producción no encontrada."));

        if (orden.getEstado() != EstadoProduccion.CREADA
                || cierreProduccionRepository.existsByOrdenProduccionId(ordenId)
                || movimientoInventarioRepository.existsByOrdenProduccionId(ordenId)
                || etapaProduccionRepository.existsByOrdenProduccionIdAndFechaInicioIsNotNull(ordenId)) {
            throw new CustomBusinessException(ApiErrorCode.OP_ETAPAS_BLOQUEADAS, MENSAJE_BLOQUEO);
        }
    }
}
