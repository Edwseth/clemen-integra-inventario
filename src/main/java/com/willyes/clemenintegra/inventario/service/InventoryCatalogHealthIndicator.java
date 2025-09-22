package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InventoryCatalogHealthIndicator implements HealthIndicator {

    private final InventoryCatalogResolver resolver;
    private final AlmacenRepository almacenRepository;
    private final MotivoMovimientoRepository motivoRepository;
    private final TipoMovimientoDetalleRepository tipoDetalleRepository;

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("almacenPt", checkAlmacen(resolver.getAlmacenPtId()));
        details.put("almacenCuarentena", checkAlmacen(resolver.getAlmacenCuarentenaId()));
        details.put("almacenObsoletos", checkAlmacen(resolver.getAlmacenObsoletosId()));
        details.put("almacenMateriaPrima", checkAlmacen(resolver.getAlmacenMateriaPrimaId()));
        details.put("almacenMaterialEmpaque", checkAlmacen(resolver.getAlmacenMaterialEmpaqueId()));
        details.put("almacenSuministros", checkAlmacen(resolver.getAlmacenSuministrosId()));
        details.put("almacenPreBodegaProduccion", checkAlmacen(resolver.getAlmacenPreBodegaProduccionId()));

        details.put("motivoEntradaPt", checkMotivo(resolver.getMotivoIdEntradaProductoTerminado()));
        details.put("motivoTransferenciaCalidad", checkMotivo(resolver.getMotivoIdTransferenciaCalidad()));
        details.put("motivoDevolucionDesdeProduccion", checkMotivo(resolver.getMotivoIdDevolucionDesdeProduccion()));
        Long ajuste = resolver.getMotivoIdAjusteRechazo();
        if (ajuste != null) {
            details.put("motivoAjusteRechazo", checkMotivo(ajuste));
        }

        details.put("tipoDetalleEntrada", checkTipoDetalle(resolver.getTipoDetalleEntradaId()));
        details.put("tipoDetalleTransferencia", checkTipoDetalle(resolver.getTipoDetalleTransferenciaId()));
        details.put("tipoDetalleSalida", checkTipoDetalle(resolver.getTipoDetalleSalidaId()));

        return Health.up().withDetails(details).build();
    }

    private Map<String, Object> checkAlmacen(Long id) {
        return Map.of("id", id, "exists", almacenRepository.existsById(id));
    }

    private Map<String, Object> checkMotivo(Long id) {
        return Map.of("id", id, "exists", motivoRepository.existsById(id));
    }

    private Map<String, Object> checkTipoDetalle(Long id) {
        return Map.of("id", id, "exists", tipoDetalleRepository.existsById(id));
    }
}
