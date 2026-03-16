package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
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

    public BigDecimal calcularEntrada(MovimientoInventario movimiento, Long almacenId) {
        ClasificacionMovimientoInventario clasificacion = movimiento.getClasificacion();
        TipoMovimiento tipoMovimiento = movimiento.getTipoMovimiento();

        if (esTransferencia(movimiento, clasificacion, tipoMovimiento)) {
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

        if (esTransferencia(movimiento, clasificacion, tipoMovimiento)) {
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

        if (esTransferencia(movimiento, clasificacion, tipo)) {
            Integer origenId = movimiento.getAlmacenOrigen() != null ? movimiento.getAlmacenOrigen().getId() : null;
            Integer destinoId = movimiento.getAlmacenDestino() != null ? movimiento.getAlmacenDestino().getId() : null;
            if (origenId != null) {
                aportes.add(new AporteInventario(origenId.longValue(), cantidad.negate()));
            }
            if (destinoId != null) {
                aportes.add(new AporteInventario(destinoId.longValue(), cantidad));
            }
            return aportes;
        }

        if (CLASIFICACIONES_ENTRADA.contains(clasificacion)
                || (clasificacion == null && (tipo == TipoMovimiento.ENTRADA
                || tipo == TipoMovimiento.RECEPCION
                || tipo == TipoMovimiento.DEVOLUCION))) {
            Long almacenId = preferirDestino(movimiento);
            if (almacenId != null) {
                aportes.add(new AporteInventario(almacenId, cantidad));
            }
            return aportes;
        }

        if (CLASIFICACIONES_SALIDA.contains(clasificacion)
                || (clasificacion == null && tipo == TipoMovimiento.SALIDA)) {
            Long almacenId = preferirOrigen(movimiento);
            if (almacenId != null) {
                aportes.add(new AporteInventario(almacenId, cantidad.negate()));
            }
        }
        return aportes;
    }

    private boolean esTransferencia(MovimientoInventario movimiento,
                                    ClasificacionMovimientoInventario clasificacion,
                                    TipoMovimiento tipoMovimiento) {
        if (CLASIFICACIONES_TRANSFERENCIA.contains(clasificacion) || tipoMovimiento == TipoMovimiento.TRANSFERENCIA) {
            return true;
        }
        return movimiento.getAlmacenOrigen() != null && movimiento.getAlmacenDestino() != null;
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

    public record AporteInventario(Long almacenId, BigDecimal cantidadFirmada) {
    }
}
