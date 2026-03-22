package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class MovimientoSignosResolver {

    private static final EnumSet<ClasificacionMovimientoInventario> CLASIFICACIONES_ENTRADA = EnumSet.of(
            ClasificacionMovimientoInventario.AJUSTE_POSITIVO,
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

    private final LoteProductoRepository loteProductoRepository;

    public MovimientoSignosResolver() {
        this.loteProductoRepository = null;
    }

    @Autowired
    public MovimientoSignosResolver(LoteProductoRepository loteProductoRepository) {
        this.loteProductoRepository = loteProductoRepository;
    }

    public BigDecimal calcularEntrada(MovimientoInventario movimiento, Long almacenId) {
        ClasificacionMovimientoInventario clasificacion = movimiento.getClasificacion();
        TipoMovimiento tipoMovimiento = movimiento.getTipoMovimiento();

        if (esDevolucionProduccionMaterializadaComoEntrada(movimiento, clasificacion, tipoMovimiento)) {
            Long almacenLote = movimiento.getLote() != null && movimiento.getLote().getAlmacen() != null
                    ? movimiento.getLote().getAlmacen().getId().longValue()
                    : null;
            if (mismoAlmacen(almacenLote, almacenId)) {
                return obtenerCantidad(movimiento);
            }
            return BigDecimal.ZERO;
        }

        if (esMovimientoEntreAlmacenes(movimiento, clasificacion, tipoMovimiento)) {
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

    public BigDecimal calcularSalida(MovimientoInventario movimiento, Long almacenId) {
        ClasificacionMovimientoInventario clasificacion = movimiento.getClasificacion();
        TipoMovimiento tipoMovimiento = movimiento.getTipoMovimiento();

        if (esDevolucionProduccionMaterializadaComoEntrada(movimiento, clasificacion, tipoMovimiento)) {
            return BigDecimal.ZERO;
        }

        if (esMovimientoEntreAlmacenes(movimiento, clasificacion, tipoMovimiento)) {
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

    public List<AporteInventario> resolverAportesPorAlmacen(MovimientoInventario movimiento) {
        BigDecimal cantidad = obtenerCantidad(movimiento);
        if (cantidad.signum() == 0) {
            return List.of();
        }

        ClasificacionMovimientoInventario clasificacion = movimiento.getClasificacion();
        TipoMovimiento tipo = movimiento.getTipoMovimiento();
        List<AporteInventario> aportes = new ArrayList<>(2);

        if (esDevolucionProduccionMaterializadaComoEntrada(movimiento, clasificacion, tipo)) {
            Long loteId = movimiento.getLote() != null ? movimiento.getLote().getId() : null;
            Long almacenLoteId = movimiento.getLote() != null && movimiento.getLote().getAlmacen() != null
                    ? movimiento.getLote().getAlmacen().getId().longValue()
                    : preferirDestino(movimiento);
            if (loteId != null && almacenLoteId != null) {
                aportes.add(new AporteInventario(loteId, almacenLoteId, cantidad));
            }
            return aportes;
        }

        if (esMovimientoEntreAlmacenes(movimiento, clasificacion, tipo)) {
            Integer origenId = movimiento.getAlmacenOrigen() != null ? movimiento.getAlmacenOrigen().getId() : null;
            Integer destinoId = movimiento.getAlmacenDestino() != null ? movimiento.getAlmacenDestino().getId() : null;
            Long loteDestinoId = resolverLotePorAlmacen(movimiento, destinoId);
            Long loteOrigenId = resolverLotePorAlmacen(movimiento, origenId);
            if (origenId != null && loteOrigenId != null) {
                aportes.add(new AporteInventario(
                        loteOrigenId,
                        origenId.longValue(),
                        cantidad.negate()
                ));
            }
            if (destinoId != null && loteDestinoId != null) {
                aportes.add(new AporteInventario(
                        loteDestinoId,
                        destinoId.longValue(),
                        cantidad
                ));
            }
            return aportes;
        }

        if (CLASIFICACIONES_ENTRADA.contains(clasificacion)
                || (clasificacion == null && (tipo == TipoMovimiento.ENTRADA
                || tipo == TipoMovimiento.RECEPCION
                || tipo == TipoMovimiento.DEVOLUCION))) {
            Long almacenId = preferirDestino(movimiento);
            if (almacenId != null) {
                aportes.add(new AporteInventario(
                        movimiento.getLote() != null ? movimiento.getLote().getId() : null,
                        almacenId,
                        cantidad
                ));
            }
            return aportes;
        }

        if (CLASIFICACIONES_SALIDA.contains(clasificacion)
                || (clasificacion == null && tipo == TipoMovimiento.SALIDA)) {
            Long almacenId = preferirOrigen(movimiento);
            if (almacenId != null) {
                aportes.add(new AporteInventario(
                        movimiento.getLote() != null ? movimiento.getLote().getId() : null,
                        almacenId,
                        cantidad.negate()
                ));
            }
        }
        return aportes;
    }

    private Long resolverLotePorAlmacen(MovimientoInventario movimiento, Integer almacenId) {
        if (movimiento == null || movimiento.getLote() == null) {
            return null;
        }
        if (almacenId == null) {
            return movimiento.getLote().getId();
        }

        if (movimiento.getLote().getAlmacen() != null
                && Objects.equals(movimiento.getLote().getAlmacen().getId(), almacenId)) {
            return movimiento.getLote().getId();
        }

        if (movimiento.getLote().getLoteOrigen() != null
                && movimiento.getLote().getLoteOrigen().getId() != null
                && movimiento.getLote().getLoteOrigen().getAlmacen() != null
                && Objects.equals(movimiento.getLote().getLoteOrigen().getAlmacen().getId(), almacenId)) {
            return movimiento.getLote().getLoteOrigen().getId();
        }

        if (loteProductoRepository != null
                && movimiento.getLote().getCodigoLote() != null
                && movimiento.getProducto() != null
                && movimiento.getProducto().getId() != null) {
            return loteProductoRepository
                    .findByCodigoLoteAndProductoIdAndAlmacenId(
                            movimiento.getLote().getCodigoLote(),
                            movimiento.getProducto().getId(),
                            almacenId)
                    .map(lote -> lote.getId())
                    .orElse(null);
        }

        return movimiento.getLote().getAlmacen() != null
                && Objects.equals(movimiento.getLote().getAlmacen().getId(), almacenId)
                ? movimiento.getLote().getId()
                : null;
    }

    private boolean esMovimientoEntreAlmacenes(MovimientoInventario movimiento,
                                               ClasificacionMovimientoInventario clasificacion,
                                               TipoMovimiento tipoMovimiento) {
        return CLASIFICACIONES_TRANSFERENCIA.contains(clasificacion)
                || (clasificacion == ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION
                && !esDevolucionProduccionMaterializadaComoEntrada(movimiento, clasificacion, tipoMovimiento))
                || tipoMovimiento == TipoMovimiento.TRANSFERENCIA;
    }

    private boolean esDevolucionProduccionMaterializadaComoEntrada(MovimientoInventario movimiento,
                                                                   ClasificacionMovimientoInventario clasificacion,
                                                                   TipoMovimiento tipoMovimiento) {
        return clasificacion == ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION
                && tipoMovimiento == TipoMovimiento.ENTRADA
                && movimiento != null
                && movimiento.getLote() != null
                && movimiento.getLote().getId() != null;
    }

    private Long preferirDestino(MovimientoInventario movimiento) {
        if (movimiento.getAlmacenDestino() != null) {
            return movimiento.getAlmacenDestino().getId().longValue();
        }
        if (movimiento.getAlmacenOrigen() != null) {
            return movimiento.getAlmacenOrigen().getId().longValue();
        }
        return null;
    }

    private Long preferirOrigen(MovimientoInventario movimiento) {
        if (movimiento.getAlmacenOrigen() != null) {
            return movimiento.getAlmacenOrigen().getId().longValue();
        }
        if (movimiento.getAlmacenDestino() != null) {
            return movimiento.getAlmacenDestino().getId().longValue();
        }
        return null;
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

    public record AporteInventario(Long loteId, Long almacenId, BigDecimal cantidadFirmada) {
    }
}
