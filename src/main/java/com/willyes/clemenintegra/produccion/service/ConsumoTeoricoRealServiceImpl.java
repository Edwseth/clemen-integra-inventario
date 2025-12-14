package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.produccion.dto.ConsumoTeoricoRealItemDTO;
import com.willyes.clemenintegra.produccion.dto.ConsumoTeoricoRealResponseDTO;
import com.willyes.clemenintegra.produccion.dto.LoteTerminadoResumenDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ConsumoTeoricoRealServiceImpl implements ConsumoTeoricoRealService {

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final FormulaProductoRepository formulaProductoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final LoteProductoRepository loteProductoRepository;

    @Override
    public ConsumoTeoricoRealResponseDTO obtenerConsumo(Long ordenProduccionId) {
        OrdenProduccion ordenProduccion = ordenProduccionRepository.findById(ordenProduccionId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ORDEN_NO_ENCONTRADA"));

        FormulaProducto formula = obtenerFormulaProducto(ordenProduccion.getProducto());
        if (formula == null || formula.getDetalles() == null || formula.getDetalles().isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.OPERACION_NO_PERMITIDA, "FORMULA_NO_ASOCIADA");
        }

        Map<Long, ConsumoTeoricoRealItemDTO.ConsumoTeoricoRealItemDTOBuilder> items = new HashMap<>();

        BigDecimal cantidadProgramada = ordenProduccion.getCantidadProgramada();
        construirTeorico(formula, cantidadProgramada, items);
        construirReal(ordenProduccionId, items);

        List<ConsumoTeoricoRealItemDTO> resultItems = items.values().stream()
                .map(builder -> {
                    ConsumoTeoricoRealItemDTO dto = builder.build();
                    BigDecimal teorica = defaultZero(dto.getCantidadTeorica());
                    BigDecimal real = defaultZero(dto.getCantidadReal());
                    BigDecimal diferencia = real.subtract(teorica);
                    dto.setDiferencia(diferencia);
                    if (teorica.compareTo(BigDecimal.ZERO) == 0) {
                        dto.setPorcentajeDesviacion(null);
                    } else {
                        dto.setPorcentajeDesviacion(diferencia
                                .divide(teorica, 6, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)));
                    }
                    return dto;
                })
                .toList();

        List<LoteTerminadoResumenDTO> lotesTerminados = obtenerLotesTerminados(ordenProduccionId);

        return ConsumoTeoricoRealResponseDTO.builder()
                .ordenId(ordenProduccion.getId())
                .codigoOrden(ordenProduccion.getCodigoOrden())
                .productoTerminadoSku(ordenProduccion.getProducto() != null
                        ? ordenProduccion.getProducto().getCodigoSku()
                        : null)
                .productoTerminadoNombre(ordenProduccion.getProducto() != null
                        ? ordenProduccion.getProducto().getNombre()
                        : null)
                .cantidadProgramada(ordenProduccion.getCantidadProgramada())
                .cantidadProducida(ordenProduccion.getCantidadProducida())
                .items(resultItems)
                .lotesTerminados(lotesTerminados)
                .build();
    }

    private void construirTeorico(FormulaProducto formula,
                                  BigDecimal cantidadProgramada,
                                  Map<Long, ConsumoTeoricoRealItemDTO.ConsumoTeoricoRealItemDTOBuilder> items) {
        for (DetalleFormula detalle : formula.getDetalles()) {
            if (esProductoSemiElaborado(detalle)) {
                expandirProductoSemiElaborado(detalle, cantidadProgramada, items);
                continue;
            }
            agregarCantidadTeorica(detalle.getInsumo(), detalle.getUnidadMedida() != null
                    ? detalle.getUnidadMedida().getNombre()
                    : null,
                    calcularCantidadTeoricaPt(detalle.getCantidadNecesaria(), cantidadProgramada),
                    items);
        }
    }

    private void construirReal(Long ordenProduccionId,
                               Map<Long, ConsumoTeoricoRealItemDTO.ConsumoTeoricoRealItemDTOBuilder> items) {
        List<MovimientoInventario> movimientos = movimientoInventarioRepository
                .findByOrdenProduccionIdAndClasificacion(
                        ordenProduccionId,
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                        Pageable.unpaged())
                .getContent();

        for (MovimientoInventario movimiento : movimientos) {
            if (movimiento.getTipoMovimiento() == null
                    || !(movimiento.getTipoMovimiento() == TipoMovimiento.SALIDA
                    || movimiento.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA
                    || movimiento.getTipoMovimiento() == TipoMovimiento.AJUSTE)) {
                continue;
            }
            Producto producto = movimiento.getProducto();
            if (producto == null || producto.getId() == null) {
                continue;
            }
            ConsumoTeoricoRealItemDTO.ConsumoTeoricoRealItemDTOBuilder builder =
                    items.computeIfAbsent(producto.getId().longValue(),
                            id -> ConsumoTeoricoRealItemDTO.builder()
                                    .productoId(id)
                                    .codigoSku(producto.getCodigoSku())
                                    .nombreProducto(producto.getNombre())
                                    .unidad(producto.getUnidadMedida() != null
                                            ? producto.getUnidadMedida().getNombre()
                                            : null)
                                    .cantidadTeorica(BigDecimal.ZERO)
                                    .cantidadReal(BigDecimal.ZERO));
            builder.cantidadReal(defaultZero(builder.build().getCantidadReal())
                    .add(defaultZero(movimiento.getCantidad())));
        }
    }

    private void agregarCantidadTeorica(Producto insumo,
                                        String unidad,
                                        BigDecimal cantidad,
                                        Map<Long, ConsumoTeoricoRealItemDTO.ConsumoTeoricoRealItemDTOBuilder> items) {
        if (insumo == null || insumo.getId() == null || cantidad == null) {
            return;
        }
        ConsumoTeoricoRealItemDTO.ConsumoTeoricoRealItemDTOBuilder builder = items
                .computeIfAbsent(insumo.getId().longValue(),
                        id -> ConsumoTeoricoRealItemDTO.builder()
                                .productoId(id)
                                .codigoSku(insumo.getCodigoSku())
                                .nombreProducto(insumo.getNombre())
                                .unidad(unidad)
                                .cantidadTeorica(BigDecimal.ZERO)
                                .cantidadReal(BigDecimal.ZERO));
        builder.cantidadTeorica(defaultZero(builder.build().getCantidadTeorica()).add(cantidad));
    }

    private void expandirProductoSemiElaborado(DetalleFormula detallePs,
                                               BigDecimal cantidadProgramada,
                                               Map<Long, ConsumoTeoricoRealItemDTO.ConsumoTeoricoRealItemDTOBuilder> items) {
        FormulaProducto formulaPs = obtenerFormulaProducto(detallePs.getInsumo());
        if (formulaPs == null || formulaPs.getDetalles() == null || formulaPs.getDetalles().isEmpty()) {
            log.warn("No se encontró fórmula activa para el producto semi elaborado {}", detallePs.getInsumo());
            agregarCantidadTeorica(detallePs.getInsumo(),
                    detallePs.getUnidadMedida() != null ? detallePs.getUnidadMedida().getNombre() : null,
                    calcularCantidadTeoricaPt(detallePs.getCantidadNecesaria(), cantidadProgramada),
                    items);
            return;
        }
        for (DetalleFormula detalleMp : formulaPs.getDetalles()) {
            BigDecimal cantidadTeorica = calcularCantidadTeoricaPs(detalleMp.getCantidadNecesaria(),
                    detallePs.getCantidadNecesaria(), cantidadProgramada);
            agregarCantidadTeorica(detalleMp.getInsumo(),
                    detalleMp.getUnidadMedida() != null ? detalleMp.getUnidadMedida().getNombre() : null,
                    cantidadTeorica,
                    items);
        }
    }

    private FormulaProducto obtenerFormulaProducto(Producto producto) {
        if (producto == null || producto.getId() == null) {
            return null;
        }
        return formulaProductoRepository
                .findByProductoIdAndEstadoAndActivoTrue(producto.getId().longValue(), EstadoFormula.APROBADA)
                .orElseGet(() -> formulaProductoRepository.findByProductoId(producto.getId().longValue()).orElse(null));
    }

    private boolean esProductoSemiElaborado(DetalleFormula detalle) {
        return detalle != null
                && detalle.getInsumo() != null
                && detalle.getInsumo().getCategoriaProducto() != null
                && detalle.getInsumo().getCategoriaProducto().getTipo() == TipoCategoria.PRODUCTO_SEMI_ELABORADO;
    }

    private BigDecimal calcularCantidadTeoricaPs(BigDecimal cantidadMpPorPs,
                                                 BigDecimal cantidadPsPorPt,
                                                 BigDecimal cantidadProgramadaPt) {
        if (cantidadMpPorPs == null) {
            return null;
        }
        BigDecimal psPorPt = cantidadPsPorPt != null ? cantidadPsPorPt : BigDecimal.ZERO;
        BigDecimal cantidadPt = cantidadProgramadaPt != null ? cantidadProgramadaPt : BigDecimal.ONE;
        return cantidadMpPorPs.multiply(psPorPt).multiply(cantidadPt);
    }

    private BigDecimal calcularCantidadTeoricaPt(BigDecimal cantidadNecesariaPorUnidad, BigDecimal cantidadProgramadaPt) {
        if (cantidadNecesariaPorUnidad == null) {
            return null;
        }
        BigDecimal cantidadPt = cantidadProgramadaPt != null ? cantidadProgramadaPt : BigDecimal.ONE;
        return cantidadNecesariaPorUnidad.multiply(cantidadPt);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private List<LoteTerminadoResumenDTO> obtenerLotesTerminados(Long ordenProduccionId) {
        List<LoteProducto> lotes = loteProductoRepository.findByOrdenProduccionId(ordenProduccionId);
        List<LoteTerminadoResumenDTO> respuesta = new ArrayList<>();
        for (LoteProducto lote : lotes) {
            respuesta.add(LoteTerminadoResumenDTO.builder()
                    .idLote(lote.getId())
                    .codigoLote(lote.getCodigoLote())
                    .estadoLote(lote.getEstado() != null ? lote.getEstado().name() : null)
                    .almacenNombre(lote.getAlmacen() != null ? lote.getAlmacen().getNombre() : null)
                    .fechaVencimiento(lote.getFechaVencimiento())
                    .stockLote(lote.getStockLote())
                    .build());
        }
        return respuesta;
    }
}

