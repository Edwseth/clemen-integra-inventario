package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryCatalogProperties;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import jakarta.annotation.PostConstruct;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryCatalogResolver {

    private final InventoryCatalogProperties properties;
    private final AlmacenRepository almacenRepository;
    private final MotivoMovimientoRepository motivoRepository;
    private final TipoMovimientoDetalleRepository tipoDetalleRepository;

    private static final Map<String, Integer> UNIDAD_DECIMALS = Map.ofEntries(
            Map.entry("UND", 0),
            Map.entry("UN", 0),
            Map.entry("UNI", 0),
            Map.entry("UNID", 0),
            Map.entry("PZA", 0),
            Map.entry("PIEZA", 0),
            Map.entry("KG", 3),
            Map.entry("L", 3),
            Map.entry("LT", 3),
            Map.entry("LB", 3),
            Map.entry("G", 6),
            Map.entry("GR", 6),
            Map.entry("ML", 6),
            Map.entry("MG", 6),
            Map.entry("CC", 6)
    );

    private Long almacenPtId;
    private Long almacenCuarentenaId;
    private Long almacenObsoletosId;
    private Long almacenMateriaPrimaId;
    private Long almacenMaterialEmpaqueId;
    private Long almacenSuministrosId;
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
        almacenMateriaPrimaId = validateAlmacen(properties.getProduccion().getAlmacen().getOrigen().getMateriaPrima());
        almacenMaterialEmpaqueId = validateAlmacen(properties.getProduccion().getAlmacen().getOrigen().getMaterialEmpaque());
        almacenSuministrosId = validateAlmacen(properties.getProduccion().getAlmacen().getOrigen().getSuministros());
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

        log.info(
                "Inventory catalogs loaded pt={} cuarentena={} obsoletos={} materiaPrima={} materialEmpaque={} suministros={} " +
                        "preBodegaProduccion={} motivoEntradaPt={} motivoTransferenciaCalidad={} motivoDevolucion={} motivoAjuste={} " +
                        "tipoDetalleEntrada={} tipoDetalleTransferencia={} tipoDetalleSalida={}",
                almacenPtId, almacenCuarentenaId, almacenObsoletosId,
                almacenMateriaPrimaId, almacenMaterialEmpaqueId, almacenSuministrosId, almacenPreBodegaProduccionId,
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
    public Long getAlmacenOrigenMateriaPrimaId() { return almacenMateriaPrimaId; }
    public Long getAlmacenOrigenMaterialEmpaqueId() { return almacenMaterialEmpaqueId; }
    public Long getAlmacenOrigenSuministrosId() { return almacenSuministrosId; }

    public Long getAlmacenMateriaPrimaId() { return getAlmacenOrigenMateriaPrimaId(); }
    public Long getAlmacenMaterialEmpaqueId() { return getAlmacenOrigenMaterialEmpaqueId(); }
    public Long getAlmacenSuministrosId() { return getAlmacenOrigenSuministrosId(); }
    public Long getAlmacenPreBodegaProduccionId() { return almacenPreBodegaProduccionId; }

    public Long getMotivoIdEntradaProductoTerminado() { return motivoEntradaPtId; }
    public Long getMotivoIdTransferenciaCalidad() { return motivoTransferenciaCalidadId; }
    public Long getMotivoIdDevolucionDesdeProduccion() { return motivoDevolucionDesdeProduccionId; }
    public Long getMotivoIdAjusteRechazo() { return motivoAjusteRechazoId; }

    public Long getTipoDetalleEntradaId() { return tipoDetalleEntradaId; }
    public Long getTipoDetalleTransferenciaId() { return tipoDetalleTransferenciaId; }
    public Long getTipoDetalleSalidaId() { return tipoDetalleSalidaId; }

    /**
     * Determina la cantidad de decimales permitidos para la unidad de medida indicada.
     * Si no existe una configuración específica, se utiliza el máximo configurado.
     */
    public int decimals(UnidadMedida unidad) {
        int max = properties.getUm().getDecimales().getMax();
        int min = properties.getUm().getDecimales().getMin();
        if (unidad == null) {
            return max;
        }
        String clave = unidad.getSimbolo() != null && !unidad.getSimbolo().isBlank()
                ? unidad.getSimbolo()
                : unidad.getNombre();
        if (clave == null) {
            return max;
        }
        String normalized = clave.trim().toUpperCase(Locale.ROOT);
        Integer configurado = UNIDAD_DECIMALS.get(normalized);
        if (configurado != null) {
            return Math.min(max, Math.max(min, configurado));
        }
        return max;
    }
}
