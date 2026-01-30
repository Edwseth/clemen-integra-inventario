package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoDetalle;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisponibilidadInsumoService {

    private final LoteProductoRepository loteProductoRepository;
    private final InventoryCatalogResolver catalogResolver;
    private final ProductoRepository productoRepository;

    private static final EnumSet<EstadoLote> ESTADOS_FEFO_PERMITIDOS =
            EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO);

    private static final Map<TipoCategoria, Function<InventoryCatalogResolver, Long>> ALMACENES_ORIGEN_POR_CATEGORIA = Map.of(
            TipoCategoria.MATERIA_PRIMA,       InventoryCatalogResolver::getAlmacenOrigenMateriaPrimaId,
            TipoCategoria.MATERIAL_EMPAQUE,    InventoryCatalogResolver::getAlmacenOrigenMaterialEmpaqueId,
            TipoCategoria.SUMINISTROS,         InventoryCatalogResolver::getAlmacenOrigenSuministrosId,
            TipoCategoria.PRODUCTO_SEMI_ELABORADO, InventoryCatalogResolver::getAlmacenOrigenProductoSemiElaboradoId);


    public List<Long> resolverAlmacenesPreferidos(Producto insumo) {
        Long preBodegaProduccionId = catalogResolver.getAlmacenPreBodegaProduccionId();
        Long origenId = Optional.ofNullable(insumo)
                .map(Producto::getCategoriaProducto)
                .map(CategoriaProducto::getTipo)
                .map(ALMACENES_ORIGEN_POR_CATEGORIA::get)
                .map(func -> func.apply(catalogResolver))
                .orElse(null);

        if (origenId == null || Objects.equals(origenId, preBodegaProduccionId)) {
            return List.of();
        }
        return List.of(origenId);
    }

    public DistribucionFefoResult calcularDisponibilidad(Long productoInsumoId,
                                                          BigDecimal cantidadRequerida,
                                                          List<Long> almacenesPreferidos,
                                                          boolean modoPreview) {
        return calcularDisponibilidad(productoInsumoId, cantidadRequerida, almacenesPreferidos, modoPreview, null, false);
    }

    public DistribucionFefoResult calcularDisponibilidad(Long productoInsumoId,
                                                          BigDecimal cantidadRequerida,
                                                          List<Long> almacenesPreferidos,
                                                          boolean modoPreview,
                                                          Long loteForzadoId,
                                                          boolean bloquearMultiplesLotes) {
        BigDecimal requerida = Optional.ofNullable(cantidadRequerida)
                .orElse(BigDecimal.ZERO)
                .setScale(8, RoundingMode.HALF_UP);

        List<Long> preferidos = almacenesPreferidos == null ? List.of() : List.copyOf(almacenesPreferidos);
        Producto producto = productoInsumoId != null
                ? productoRepository.findById(productoInsumoId).orElse(null)
                : null;
        ModoControlInventario modoControl = Optional.ofNullable(producto)
                .map(Producto::getModoControlInventario)
                .orElse(ModoControlInventario.CONTROL_STOCK);

        if (modoControl == ModoControlInventario.SIN_CONTROL_STOCK) {
            BigDecimal requeridaEscala = requerida.setScale(6, RoundingMode.HALF_UP);
            BigDecimal stockLibreSimulado = requeridaEscala;
            // Insumos sin control de inventario (ej. agua por tubería) no bloquean la OP ni consultan lotes
            return DistribucionFefoResult.builder()
                    .productoInsumoId(productoInsumoId)
                    .requerido(requeridaEscala)
                    .stockFisicoTotal(requeridaEscala)
                    .stockReservadoTotal(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                    .stockLibreTotal(stockLibreSimulado)
                    .faltante(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                    .suficiente(true)
                    .usoFallback(false)
                    .almacenesPreferidos(preferidos)
                    .detalles(List.of())
                    .build();
        }

        List<LoteFefoDisponibleProjection> lotesDisponibles = loteProductoRepository
                .findFefoDisponibles(productoInsumoId, Integer.MAX_VALUE);
        Long preBodegaId = catalogResolver.getAlmacenPreBodegaProduccionId();
        if (preBodegaId != null) {
            lotesDisponibles = lotesDisponibles.stream()
                    .filter(lote -> lote.getAlmacenId() == null || !Objects.equals(lote.getAlmacenId(), preBodegaId))
                    .toList();
        }

        EnumSet<EstadoLote> estadosPermitidos = obtenerEstadosPermitidos(producto);
        List<LoteFefoDisponibleProjection> lotesElegibles = lotesDisponibles.stream()
                .filter(l -> esLotePermitido(l, estadosPermitidos))
                .filter(l -> loteForzadoId == null || Objects.equals(l.getLoteProductoId(), loteForzadoId))
                .toList();

        BigDecimal stockLibreElegible = lotesElegibles.stream()
                .map(this::calcularStockLibre)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (preBodegaId != null && stockLibreElegible.compareTo(requerida) < 0) {
            throw new CustomBusinessException(
                    ApiErrorCode.PREBODEGA_ORIGEN_INVALIDO,
                    "STOCK_INSUFICIENTE_ORIGEN: la Pre-Bodega Producción no se considera origen de insumos",
                    Map.of(
                            "productoInsumoId", productoInsumoId,
                            "preBodegaId", preBodegaId,
                            "requerido", requerida,
                            "stockLibreElegible", stockLibreElegible
                    ));
        }

        List<LoteFefoDisponibleProjection> lotesSeleccionados = new ArrayList<>(lotesElegibles);
        boolean usoFallback = false;
        String motivoFallback = null;

        if (loteForzadoId != null) {
            usoFallback = false;
            motivoFallback = null;
            lotesSeleccionados = lotesElegibles.stream()
                    .filter(l -> Objects.equals(l.getLoteProductoId(), loteForzadoId))
                    .toList();
        } else if (!preferidos.isEmpty()) {
            lotesSeleccionados = lotesElegibles.stream()
                    .filter(lote -> lote.getAlmacenId() != null && preferidos.contains(lote.getAlmacenId()))
                    .toList();

            BigDecimal cubierto = lotesSeleccionados.stream()
                    .map(this::calcularStockLibre)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (lotesSeleccionados.isEmpty() || cubierto.compareTo(requerida) < 0) {
                usoFallback = true;
                motivoFallback = lotesSeleccionados.isEmpty() ? "SIN_ALMACEN_CONFIGURADO" : "STOCK_NO_DISPONIBLE_EN_ORIGEN";
                lotesSeleccionados = lotesElegibles;
            }
        } else if (!lotesElegibles.isEmpty()) {
            usoFallback = true;
            motivoFallback = "SIN_ALMACEN_CONFIGURADO";
        }

        if (bloquearMultiplesLotes && !lotesSeleccionados.isEmpty()) {
            lotesSeleccionados = List.of(lotesSeleccionados.get(0));
        }

        if (usoFallback && !modoPreview) {
            log.info("Disponibilidad FEFO fallback insumoId={} requerida={} motivo={} almacenesPreferidos={}",
                    productoInsumoId, requerida, motivoFallback, preferidos);
        }

        lotesDisponibles.stream()
                .map(LoteFefoDisponibleProjection::getEstado)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::parseEstadoLoteSafe)
                .filter(Objects::nonNull)
                .filter(estado -> !estadosPermitidos.contains(estado))
                .findFirst()
                .ifPresent(estado -> log.warn("Disponibilidad FEFO detectó lote en estado no permitido: {}", estado));

        BigDecimal stockFisicoTotal = sumar(lotesElegibles, LoteFefoDisponibleProjection::getStockFisico);
        BigDecimal stockReservadoTotal = sumar(lotesElegibles, LoteFefoDisponibleProjection::getStockReservado);
        BigDecimal stockLibreTotal = lotesElegibles.stream()
                .map(this::calcularStockLibre)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        List<DistribucionFefoDetalle> detalles = new ArrayList<>();
        BigDecimal restante = requerida;
        for (LoteFefoDisponibleProjection lote : lotesSeleccionados) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal disponible = calcularStockLibre(lote);
            if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal usar = disponible.min(restante);
            if (usar.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal usarCalculo = usar.setScale(8, RoundingMode.HALF_UP);
            BigDecimal usarDetalle = usarCalculo.setScale(6, RoundingMode.HALF_UP);

            detalles.add(DistribucionFefoDetalle.builder()
                    .loteProductoId(lote.getLoteProductoId())
                    .codigoLote(lote.getCodigoLote())
                    .almacenId(lote.getAlmacenId())
                    .cantidadCalculo(usarCalculo)
                    .cantidadReserva(usarDetalle)
                    .disponible(disponible)
                    .estado(lote.getEstado())
                    .build());

            restante = restante.subtract(usarCalculo).setScale(8, RoundingMode.HALF_UP);
        }

        if (restante.compareTo(BigDecimal.ZERO) < 0) {
            restante = BigDecimal.ZERO;
        }

        BigDecimal faltante = restante.max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
        boolean suficiente = faltante.compareTo(BigDecimal.ZERO) == 0;

        if (productoInsumoId != null
                && stockFisicoTotal.compareTo(requerida) >= 0
                && stockLibreTotal.compareTo(requerida) < 0) {
            String codigo = Optional.ofNullable(producto)
                    .map(Producto::getCodigoSku)
                    .orElseGet(() -> productoRepository.findById(productoInsumoId)
                            .map(Producto::getCodigoSku)
                            .orElse(null));
            log.warn("Disponibilidad FEFO detectó stock libre insuficiente pese a stock físico: insumoId={} codigo={} requerido={} stockFisicoTotal={} stockReservadoTotal={} stockLibreFefo={} faltante={} almacenes={}",
                    productoInsumoId,
                    codigo,
                    requerida,
                    stockFisicoTotal,
                    stockReservadoTotal,
                    stockLibreTotal,
                    faltante,
                    preferidos);
        }

        return DistribucionFefoResult.builder()
                .productoInsumoId(productoInsumoId)
                .requerido(requerida.setScale(6, RoundingMode.HALF_UP))
                .stockFisicoTotal(stockFisicoTotal)
                .stockReservadoTotal(stockReservadoTotal)
                .stockLibreTotal(stockLibreTotal)
                .faltante(faltante)
                .suficiente(suficiente)
                .usoFallback(usoFallback)
                .almacenesPreferidos(preferidos)
                .detalles(detalles)
                .build();
    }

    private BigDecimal sumar(List<LoteFefoDisponibleProjection> lotes,
                              Function<LoteFefoDisponibleProjection, BigDecimal> extractor) {
        BigDecimal total = lotes.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(6, RoundingMode.HALF_UP);
    }

    private EnumSet<EstadoLote> obtenerEstadosPermitidos(Producto producto) {
        // Regla única basada en banderas por disciplina.
        // - Si el producto requiere algún análisis de calidad, SOLO se pueden consumir lotes LIBERADOS.
        // - Si no requiere análisis, se aplican los estados "normales" permitidos (DISPONIBLE y LIBERADO).
        if (producto != null && (producto.isRequiereAnalisisFisico()
                || producto.isRequiereAnalisisQuimico()
                || producto.isRequiereAnalisisMicrobiologico())) {
            return EnumSet.of(EstadoLote.LIBERADO);
        }
        return ESTADOS_FEFO_PERMITIDOS;
    }


    private boolean esLotePermitido(LoteFefoDisponibleProjection lote, EnumSet<EstadoLote> estadosPermitidos) {
        EstadoLote estado = parseEstadoLoteSafe(lote.getEstado());
        return estado != null && estadosPermitidos.contains(estado);
    }

    private BigDecimal calcularStockLibre(LoteFefoDisponibleProjection lote) {
        BigDecimal stockBase = Optional.ofNullable(lote.getStockLote())
                .orElse(Optional.ofNullable(lote.getStockFisico()).orElse(BigDecimal.ZERO));
        if (stockBase.compareTo(BigDecimal.ZERO) < 0) {
            stockBase = BigDecimal.ZERO;
        }
        return stockBase.setScale(8, RoundingMode.HALF_UP);
    }

    private EstadoLote parseEstadoLoteSafe(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return EstadoLote.valueOf(valor.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Estado de lote desconocido recibido en FEFO: {}", valor);
            return null;
        }
    }
}
