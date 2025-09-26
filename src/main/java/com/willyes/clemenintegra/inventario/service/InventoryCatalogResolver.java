package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryCatalogProperties;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    private Long almacenPtId;
    private Long almacenCuarentenaId;
    private Long almacenObsoletosId;
    private Long almacenMateriaPrimaId;
    private Long almacenMaterialEmpaqueId;
    private Long almacenSuministrosId;
    private Long almacenPreBodegaProduccionId;

    private Long motivoEntradaPtId;
    private Long motivoSalidaProduccionId;
    private Long motivoTransferenciaCalidadId;
    private Long motivoDevolucionDesdeProduccionId;
    private Long motivoAjusteRechazoId;

    private Long tipoDetalleEntradaId;
    private Long tipoDetalleTransferenciaId;
    private Long tipoDetalleSalidaId;
    private Long tipoDetalleSalidaPtId;
    private Long tipoDetalleSalidaProduccionId;

    private static final Map<String, Integer> DECIMALES_POR_UNIDAD = Map.ofEntries(
            Map.entry("UND", 0),
            Map.entry("UN", 0),
            Map.entry("UNIDAD", 0),
            Map.entry("PZA", 0),
            Map.entry("PIEZA", 0),
            Map.entry("CAJA", 0),
            Map.entry("BOLSA", 0),
            Map.entry("KG", 3),
            Map.entry("KILOGRAMO", 3),
            Map.entry("LB", 3),
            Map.entry("G", 3),
            Map.entry("GR", 3),
            Map.entry("GRAMO", 3),
            Map.entry("L", 3),
            Map.entry("LT", 3),
            Map.entry("LITRO", 3),
            Map.entry("MG", 6),
            Map.entry("MILIGRAMO", 6),
            Map.entry("ML", 6),
            Map.entry("MILILITRO", 6)
    );

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
        motivoSalidaProduccionId = resolveMotivo(properties.getMotivo().getSalidaProduccion());
        Long ajusteId = properties.getMov().getMotivo().getAjusteRechazo();
        if (ajusteId != null) {
            motivoAjusteRechazoId = validateMotivo(ajusteId);
        }

        tipoDetalleEntradaId = validateTipoDetalle(properties.getTipoDetalle().getEntradaId());
        tipoDetalleTransferenciaId = validateTipoDetalle(properties.getTipoDetalle().getTransferenciaId());
        tipoDetalleSalidaId = validateTipoDetalle(properties.getTipoDetalle().getSalidaId());
        Long salidaPtConfig = properties.getTipoDetalle().getSalidaPtId();
        if (salidaPtConfig != null) {
            tipoDetalleSalidaPtId = validateTipoDetalle(salidaPtConfig);
        }

        log.info(
                "Inventory catalogs loaded pt={} cuarentena={} obsoletos={} materiaPrima={} materialEmpaque={} suministros={} " +
                        "preBodegaProduccion={} motivoEntradaPt={} motivoTransferenciaCalidad={} motivoDevolucion={} motivoAjuste={} " +
                        "tipoDetalleEntrada={} tipoDetalleTransferencia={} tipoDetalleSalida={} tipoDetalleSalidaPt={}",
                almacenPtId, almacenCuarentenaId, almacenObsoletosId,
                almacenMateriaPrimaId, almacenMaterialEmpaqueId, almacenSuministrosId, almacenPreBodegaProduccionId,
                motivoEntradaPtId, motivoTransferenciaCalidadId, motivoDevolucionDesdeProduccionId,
                motivoAjusteRechazoId, tipoDetalleEntradaId, tipoDetalleTransferenciaId, tipoDetalleSalidaId,
                getTipoDetalleSalidaPtId());
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
    public Long getMotivoSalidaProduccionId() {return motivoSalidaProduccionId;}

    public Long getTipoDetalleSalidaProduccionId() {return tipoDetalleSalidaProduccionId;}
    public Long getTipoDetalleEntradaId() { return tipoDetalleEntradaId; }
    public Long getTipoDetalleTransferenciaId() { return tipoDetalleTransferenciaId; }
    public Long getTipoDetalleSalidaId() { return tipoDetalleSalidaId; }

    public Long getTipoDetalleSalidaPtId() {
        return tipoDetalleSalidaPtId != null ? tipoDetalleSalidaPtId : tipoDetalleSalidaId;
    }

    public Long resolveAlmacenPrincipal(Producto producto) {
        if (producto == null
                || producto.getCategoriaProducto() == null
                || producto.getCategoriaProducto().getTipo() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_SIN_CATEGORIA");
        }
        TipoCategoria tipo = producto.getCategoriaProducto().getTipo();
        return switch (tipo) {
            case MATERIA_PRIMA -> getAlmacenMateriaPrimaId();
            case MATERIAL_EMPAQUE -> getAlmacenMaterialEmpaqueId();
            case PRODUCTO_TERMINADO -> getAlmacenPtId();
            default -> throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "TIPO_CATEGORIA_SIN_ALMACEN (tipo=" + tipo + ")");
        };
    }

    public int decimals(UnidadMedida unidad) {
        int min = Math.max(0, properties.getUm().getDecimales().getMin());
        int max = Math.max(min, properties.getUm().getDecimales().getMax());
        if (unidad == null) {
            return min;
        }

        int resolved = resolveDecimalsFromUnidad(unidad, min);
        if (resolved < min) {
            resolved = min;
        }
        if (resolved > max) {
            resolved = max;
        }
        return resolved;
    }

    private int resolveDecimalsFromUnidad(UnidadMedida unidad, int defaultValue) {
        String simbolo = normalizarUnidad(unidad.getSimbolo());
        if (!simbolo.isEmpty()) {
            Integer exact = DECIMALES_POR_UNIDAD.get(simbolo);
            if (exact != null) {
                return exact;
            }
            if (simbolo.contains("MG") || simbolo.contains("MICRO")) {
                return Math.max(defaultValue, 6);
            }
            if (simbolo.contains("G") || simbolo.contains("KG") || simbolo.contains("L")) {
                return Math.max(defaultValue, 3);
            }
        }

        String nombre = normalizarUnidad(unidad.getNombre());
        if (!nombre.isEmpty()) {
            Integer exact = DECIMALES_POR_UNIDAD.get(nombre);
            if (exact != null) {
                return exact;
            }
            if (nombre.contains("MILIGRAM") || nombre.contains("MICRO")) {
                return Math.max(defaultValue, 6);
            }
            if (nombre.contains("GRAM") || nombre.contains("LITR") || nombre.contains("KILO")) {
                return Math.max(defaultValue, 3);
            }
        }
        return defaultValue;
    }

    private String normalizarUnidad(String valor) {
        if (valor == null) {
            return "";
        }
        return java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .trim();
    }
}
