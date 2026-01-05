package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.KardexFiltro;
import com.willyes.clemenintegra.inventario.dto.KardexItemDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KardexServiceImpl implements KardexService {

    private static final EnumSet<ClasificacionMovimientoInventario> CLASIFICACIONES_ENTRADA = EnumSet.of(
            ClasificacionMovimientoInventario.AJUSTE_POSITIVO,
            ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION,
            ClasificacionMovimientoInventario.DEVOLUCION_DE_PROVEEDOR,
            ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO,
            ClasificacionMovimientoInventario.ENTRADA_REPROCESO,
            ClasificacionMovimientoInventario.RECEPCION_COMPRA,
            ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE,
            ClasificacionMovimientoInventario.LIBERACION_CALIDAD
    );

    private static final EnumSet<ClasificacionMovimientoInventario> CLASIFICACIONES_SALIDA = EnumSet.of(
            ClasificacionMovimientoInventario.AJUSTE_NEGATIVO,
            ClasificacionMovimientoInventario.DEVOLUCION_A_PROVEEDOR,
            ClasificacionMovimientoInventario.SALIDA_MUESTRA_CALIDAD,
            ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
            ClasificacionMovimientoInventario.SALIDA_CLIENTE,
            ClasificacionMovimientoInventario.RECHAZO_CALIDAD
    );

    private static final EnumSet<ClasificacionMovimientoInventario> CLASIFICACIONES_TRANSFERENCIA = EnumSet.of(
            ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
            ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION
    );

    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    @Override
    public List<KardexItemDTO> obtenerKardex(KardexFiltro filtro) {
        if ((filtro.getProductoId() == null) && !StringUtils.hasText(filtro.getCodigoSku())) {
            throw new IllegalArgumentException("Se requiere productoId o codigoSku");
        }

        Producto producto = resolverProducto(filtro.getProductoId(), filtro.getCodigoSku());
        Long productoId = producto.getId() != null ? producto.getId().longValue() : null;
        LoteProducto lote = resolverLote(filtro.getLoteId(), filtro.getCodigoLote(), productoId);

        List<MovimientoInventario> movimientos = movimientoInventarioRepository.buscarParaKardex(
                filtro.getFechaDesde(),
                filtro.getFechaHasta(),
                productoId,
                lote != null ? lote.getId() : null,
                filtro.getAlmacenId(),
                filtro.getOrdenProduccionId(),
                filtro.getEtapaProduccionId()
        );

        return calcularSaldo(movimientos, producto, lote, filtro.getAlmacenId());
    }

    private Producto resolverProducto(Long productoId, String codigoSku) {
        if (productoId != null) {
            return productoRepository.findById(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        }
        return productoRepository.findByCodigoSku(codigoSku)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    private LoteProducto resolverLote(Long loteId, String codigoLote, Long productoId) {
        if (loteId != null) {
            Optional<LoteProducto> lote = loteProductoRepository.findById(loteId);
            return lote.filter(lp -> Objects.equals(lp.getProducto() != null ? lp.getProducto().getId().longValue() : null, productoId))
                    .orElseThrow(() -> new IllegalArgumentException("Lote no pertenece al producto"));
        }
        if (StringUtils.hasText(codigoLote)) {
            return loteProductoRepository.findByCodigoLoteAndProductoId(codigoLote, productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));
        }
        return null;
    }

    private List<KardexItemDTO> calcularSaldo(List<MovimientoInventario> movimientos,
                                              Producto producto,
                                              LoteProducto lote,
                                              Long almacenId) {
        BigDecimal saldo = BigDecimal.ZERO;
        List<MovimientoConTotales> items = movimientos.stream()
                .map(mov -> new MovimientoConTotales(mov, calcularEntrada(mov, almacenId), calcularSalida(mov, almacenId)))
                .collect(Collectors.toList());

        List<KardexItemDTO> resultado = new java.util.ArrayList<>(items.size());
        for (MovimientoConTotales item : items) {
            saldo = saldo.add(item.entrada()).subtract(item.salida());
            MovimientoInventario mov = item.movimiento();
            resultado.add(KardexItemDTO.builder()
                    .fechaMovimiento(mov.getFechaIngreso())
                    .tipoMovimiento(mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().name() : null)
                    .clasificacion(mov.getClasificacion() != null ? mov.getClasificacion().name() : null)
                    .referencia(mov.getDocReferencia())
                    .almacenOrigen(mov.getAlmacenOrigen() != null ? mov.getAlmacenOrigen().getNombre() : null)
                    .almacenDestino(mov.getAlmacenDestino() != null ? mov.getAlmacenDestino().getNombre() : null)
                    .codigoLote(mov.getLote() != null ? mov.getLote().getCodigoLote() : (lote != null ? lote.getCodigoLote() : null))
                    .codigoSku(producto.getCodigoSku())
                    .nombreProducto(producto.getNombre())
                    .cantidadEntrada(item.entrada())
                    .cantidadSalida(item.salida())
                    .saldo(saldo)
                    .usuario(mov.getRegistradoPor() != null ? mov.getRegistradoPor().getNombreCompleto() : null)
                    .build());
        }
        return resultado;
    }

    private record MovimientoConTotales(MovimientoInventario movimiento, BigDecimal entrada, BigDecimal salida) {}

    private BigDecimal calcularEntrada(MovimientoInventario movimiento, Long almacenId) {
        ClasificacionMovimientoInventario clasificacion = movimiento.getClasificacion();
        TipoMovimiento tipoMovimiento = movimiento.getTipoMovimiento();

        if (CLASIFICACIONES_TRANSFERENCIA.contains(clasificacion) || tipoMovimiento == TipoMovimiento.TRANSFERENCIA) {
            if (mismoAlmacen(movimiento.getAlmacenDestino() != null ? movimiento.getAlmacenDestino().getId() : null, almacenId)) {
                return obtenerCantidad(movimiento);
            }
            return BigDecimal.ZERO;
        }

        if (CLASIFICACIONES_ENTRADA.contains(clasificacion)) {
            return obtenerCantidad(movimiento);
        }

        if (clasificacion == null && (tipoMovimiento == TipoMovimiento.ENTRADA
                || tipoMovimiento == TipoMovimiento.RECEPCION
                || tipoMovimiento == TipoMovimiento.DEVOLUCION)) {
            return obtenerCantidad(movimiento);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularSalida(MovimientoInventario movimiento, Long almacenId) {
        ClasificacionMovimientoInventario clasificacion = movimiento.getClasificacion();
        TipoMovimiento tipoMovimiento = movimiento.getTipoMovimiento();

        if (CLASIFICACIONES_TRANSFERENCIA.contains(clasificacion) || tipoMovimiento == TipoMovimiento.TRANSFERENCIA) {
            if (mismoAlmacen(movimiento.getAlmacenOrigen() != null ? movimiento.getAlmacenOrigen().getId() : null, almacenId)) {
                return obtenerCantidad(movimiento);
            }
            return BigDecimal.ZERO;
        }

        if (CLASIFICACIONES_SALIDA.contains(clasificacion)) {
            return obtenerCantidad(movimiento);
        }

        if (clasificacion == null && tipoMovimiento == TipoMovimiento.SALIDA) {
            return obtenerCantidad(movimiento);
        }

        return BigDecimal.ZERO;
    }

    private boolean mismoAlmacen(Number almacenMovimientoId, Long almacenFiltroId) {
        if (almacenMovimientoId == null || almacenFiltroId == null) {
            return false;
        }
        return Objects.equals(almacenMovimientoId.longValue(), almacenFiltroId);
    }

    private BigDecimal obtenerCantidad(MovimientoInventario movimientoInventario) {
        return Optional.ofNullable(movimientoInventario.getCantidad()).orElse(BigDecimal.ZERO);
    }
}
