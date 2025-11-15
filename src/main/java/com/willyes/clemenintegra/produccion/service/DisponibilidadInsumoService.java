package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoDetalle;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
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

    private static final EnumSet<EstadoLote> ESTADOS_FEFO_PERMITIDOS = EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO);
    private static final Map<TipoCategoria, Function<InventoryCatalogResolver, Long>> ALMACENES_ORIGEN_POR_CATEGORIA = Map.of(
            TipoCategoria.MATERIA_PRIMA, InventoryCatalogResolver::getAlmacenOrigenMateriaPrimaId,
            TipoCategoria.MATERIAL_EMPAQUE, InventoryCatalogResolver::getAlmacenOrigenMaterialEmpaqueId,
            TipoCategoria.SUMINISTROS, InventoryCatalogResolver::getAlmacenOrigenSuministrosId);

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
        BigDecimal requerida = Optional.ofNullable(cantidadRequerida)
                .orElse(BigDecimal.ZERO)
                .setScale(8, RoundingMode.HALF_UP);

        List<LoteFefoDisponibleProjection> lotesDisponibles = loteProductoRepository
                .findFefoDisponibles(productoInsumoId, Integer.MAX_VALUE);

        List<Long> preferidos = almacenesPreferidos == null ? List.of() : List.copyOf(almacenesPreferidos);
        List<LoteFefoDisponibleProjection> lotesSeleccionados = new ArrayList<>(lotesDisponibles);
        boolean usoFallback = false;
        String motivoFallback = null;

        if (!preferidos.isEmpty()) {
            lotesSeleccionados = lotesDisponibles.stream()
                    .filter(lote -> lote.getAlmacenId() != null && preferidos.contains(lote.getAlmacenId()))
                    .toList();

            BigDecimal cubierto = lotesSeleccionados.stream()
                    .map(LoteFefoDisponibleProjection::getStockLote)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (lotesSeleccionados.isEmpty() || cubierto.compareTo(requerida) < 0) {
                usoFallback = true;
                motivoFallback = lotesSeleccionados.isEmpty() ? "SIN_ALMACEN_CONFIGURADO" : "STOCK_NO_DISPONIBLE_EN_ORIGEN";
                lotesSeleccionados = lotesDisponibles;
            }
        } else if (!lotesDisponibles.isEmpty()) {
            usoFallback = true;
            motivoFallback = "SIN_ALMACEN_CONFIGURADO";
        }

        if (usoFallback && !modoPreview) {
            log.info("Disponibilidad FEFO fallback insumoId={} requerida={} motivo={} almacenesPreferidos={}",
                    productoInsumoId, requerida, motivoFallback, preferidos);
        }

        lotesSeleccionados.stream()
                .map(LoteFefoDisponibleProjection::getEstado)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::parseEstadoLoteSafe)
                .filter(Objects::nonNull)
                .filter(estado -> !ESTADOS_FEFO_PERMITIDOS.contains(estado))
                .findFirst()
                .ifPresent(estado -> log.warn("Disponibilidad FEFO detectó lote en estado no permitido: {}", estado));

        BigDecimal stockFisicoTotal = sumar(lotesDisponibles, LoteFefoDisponibleProjection::getStockFisico);
        BigDecimal stockReservadoTotal = sumar(lotesDisponibles, LoteFefoDisponibleProjection::getStockReservado);
        BigDecimal stockLibreTotal = sumar(lotesDisponibles, LoteFefoDisponibleProjection::getStockLote);

        List<DistribucionFefoDetalle> detalles = new ArrayList<>();
        BigDecimal restante = requerida;
        for (LoteFefoDisponibleProjection lote : lotesSeleccionados) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal disponible = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
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

        BigDecimal faltante = restante.setScale(6, RoundingMode.HALF_UP);
        boolean suficiente = faltante.compareTo(BigDecimal.ZERO) <= 0;

        if (productoInsumoId != null
                && stockFisicoTotal.compareTo(requerida) >= 0
                && stockLibreTotal.compareTo(requerida) < 0) {
            String codigo = productoRepository.findById(productoInsumoId)
                    .map(Producto::getCodigoSku)
                    .orElse(null);
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
        return lotes.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private EstadoLote parseEstadoLoteSafe(String valor) {
        try {
            return EstadoLote.valueOf(valor.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Estado de lote desconocido recibido en FEFO: {}", valor);
            return null;
        }
    }
}
