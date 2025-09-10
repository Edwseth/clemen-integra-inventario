package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryCatalogProperties;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryCatalogResolver {

    private final InventoryCatalogProperties properties;
    private final AlmacenRepository almacenRepository;
    private final MotivoMovimientoRepository motivoRepository;
    private final TipoMovimientoDetalleRepository tipoDetalleRepository;

    private Long almacenPtId;
    private Long almacenCuarentenaId;
    private Long almacenObsoletosId;
    private Long almacenBodegaPrincipalId;
    private Long almacenPreBodegaProduccionId;

    private Long motivoEntradaPtId;
    private Long motivoTransferenciaCalidadId;
    private Long motivoDevolucionDesdeProduccionId;
    private Long motivoAjusteRechazoId;

    private Long tipoDetalleEntradaId;
    private Long tipoDetalleTransferenciaId;
    private Long tipoDetalleSalidaId;

    @PostConstruct
    public void init() {
        almacenPtId = validateAlmacen(properties.getAlmacen().getPt().getId());
        almacenCuarentenaId = validateAlmacen(properties.getAlmacen().getCuarentena().getId());
        almacenObsoletosId = validateAlmacen(properties.getAlmacen().getObsoletos().getId());
        almacenBodegaPrincipalId = validateAlmacen(properties.getProduccion().getAlmacen().getOrigen().getBodegaPrincipal());
        almacenPreBodegaProduccionId = validateAlmacen(properties.getProduccion().getAlmacen().getOrigen().getPreBodegaProduccion());

        motivoEntradaPtId = resolveMotivo(properties.getMotivo().getEntradaPt());
        motivoTransferenciaCalidadId = resolveMotivo(properties.getMotivo().getTransferenciaCalidad());
        motivoDevolucionDesdeProduccionId = resolveMotivo(properties.getMotivo().getDevolucionDesdeProduccion());
        Long ajusteId = properties.getMov().getMotivo().getAjusteRechazo();
        if (ajusteId != null) {
            motivoAjusteRechazoId = validateMotivo(ajusteId);
        }

        tipoDetalleEntradaId = validateTipoDetalle(properties.getTipoDetalle().getEntradaId());
        tipoDetalleTransferenciaId = validateTipoDetalle(properties.getTipoDetalle().getTransferenciaId());
        tipoDetalleSalidaId = validateTipoDetalle(properties.getTipoDetalle().getSalidaId());

        log.info("Inventory catalogs loaded pt={} cuarentena={} obsoletos={} bodegaPrincipal={} preBodegaProduccion={} motivoEntradaPt={} motivoTransferenciaCalidad={} motivoDevolucion={} motivoAjuste={} tipoDetalleEntrada={} tipoDetalleTransferencia={} tipoDetalleSalida={}",
                almacenPtId, almacenCuarentenaId, almacenObsoletosId, almacenBodegaPrincipalId, almacenPreBodegaProduccionId,
                motivoEntradaPtId, motivoTransferenciaCalidadId, motivoDevolucionDesdeProduccionId,
                motivoAjusteRechazoId, tipoDetalleEntradaId, tipoDetalleTransferenciaId, tipoDetalleSalidaId);
    }

    private Long validateAlmacen(Long id) {
        if (id == null || !almacenRepository.existsById(id)) {
            throw new IllegalStateException("CONFIG_ALMACEN_INEXISTENTE (id=" + id + ")");
        }
        return id;
    }

    private Long validateTipoDetalle(Long id) {
        if (id == null || !tipoDetalleRepository.existsById(id)) {
            throw new IllegalStateException("CONFIG_TIPO_DETALLE_INEXISTENTE (id=" + id + ")");
        }
        return id;
    }

    private Long validateMotivo(Long id) {
        if (id == null || !motivoRepository.existsById(id)) {
            throw new IllegalStateException("CONFIG_MOTIVO_INEXISTENTE (id=" + id + ")");
        }
        return id;
    }

    private Long resolveMotivo(String clave) {
        try {
            var clasif = ClasificacionMovimientoInventario.valueOf(clave);
            return motivoRepository.findByMotivo(clasif)
                    .orElseThrow(() -> new IllegalStateException("CONFIG_MOTIVO_INEXISTENTE (motivo=" + clave + ")"))
                    .getId();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("CONFIG_MOTIVO_INEXISTENTE (motivo=" + clave + ")");
        }
    }

    public Long getAlmacenPtId() { return almacenPtId; }
    public Long getAlmacenCuarentenaId() { return almacenCuarentenaId; }
    public Long getAlmacenObsoletosId() { return almacenObsoletosId; }
    public Long getAlmacenBodegaPrincipalId() { return almacenBodegaPrincipalId; }
    public Long getAlmacenPreBodegaProduccionId() { return almacenPreBodegaProduccionId; }

    public Long getMotivoIdEntradaProductoTerminado() { return motivoEntradaPtId; }
    public Long getMotivoIdTransferenciaCalidad() { return motivoTransferenciaCalidadId; }
    public Long getMotivoIdDevolucionDesdeProduccion() { return motivoDevolucionDesdeProduccionId; }
    public Long getMotivoIdAjusteRechazo() { return motivoAjusteRechazoId; }

    public Long getTipoDetalleEntradaId() { return tipoDetalleEntradaId; }
    public Long getTipoDetalleTransferenciaId() { return tipoDetalleTransferenciaId; }
    public Long getTipoDetalleSalidaId() { return tipoDetalleSalidaId; }
}
