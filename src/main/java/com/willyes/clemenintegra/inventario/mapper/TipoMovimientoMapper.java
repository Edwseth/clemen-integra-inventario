package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimientoDetalle;

import java.util.EnumMap;
import java.util.Map;

public class TipoMovimientoMapper {

    private static final Map<TipoMovimientoDetalle, TipoMovimiento> mapa = new EnumMap<>(TipoMovimientoDetalle.class);

    static {
        mapa.put(TipoMovimientoDetalle.RECEPCION_COMPRA, TipoMovimiento.RECEPCION_COMPRA);
        mapa.put(TipoMovimientoDetalle.RECEPCION_DEVOLUCION_CLIENTE, TipoMovimiento.ENTRADA_ASEGURAMIENTO_CALIDAD);
        mapa.put(TipoMovimientoDetalle.AJUSTE_POSITIVO, TipoMovimiento.AJUSTE_POSITIVO);
        mapa.put(TipoMovimientoDetalle.AJUSTE_NEGATIVO, TipoMovimiento.AJUSTE_NEGATIVO);
        mapa.put(TipoMovimientoDetalle.SALIDA_PRODUCCION, TipoMovimiento.SALIDA_PRODUCCION);
        mapa.put(TipoMovimientoDetalle.ENTRADA_PRODUCCION, TipoMovimiento.ENTRADA_PRODUCCION);
        mapa.put(TipoMovimientoDetalle.TRANSFERENCIA_ENTRADA, TipoMovimiento.TRANSFERENCIA_ENTRADA);
        mapa.put(TipoMovimientoDetalle.TRANSFERENCIA_SALIDA, TipoMovimiento.TRANSFERENCIA_SALIDA);
        mapa.put(TipoMovimientoDetalle.DEVOLUCION_PROVEEDOR, TipoMovimiento.SALIDA_PRODUCCION);
        mapa.put(TipoMovimientoDetalle.SALIDA_MUESTRA, TipoMovimiento.SALIDA_ASEGURAMIENTO_CALIDAD);
        mapa.put(TipoMovimientoDetalle.SALIDA_OBSOLETO, TipoMovimiento.SALIDA_RETIRO_MERCADO);
        mapa.put(TipoMovimientoDetalle.SALIDA_DONACION, TipoMovimiento.SALIDA_DONACION);
        mapa.put(TipoMovimientoDetalle.SALIDA_RETIRO_MERCADO, TipoMovimiento.SALIDA_RETIRO_MERCADO);
        mapa.put(TipoMovimientoDetalle.SALIDA_PERDIDA, TipoMovimiento.SALIDA_PERDIDA);
        mapa.put(TipoMovimientoDetalle.SALIDA_VENCIDO, TipoMovimiento.SALIDA_VENCIDO);
        mapa.put(TipoMovimientoDetalle.SALIDA_MANTENIMIENTO, TipoMovimiento.SALIDA_MANTENIMIENTO);
        mapa.put(TipoMovimientoDetalle.ENTRADA_REPROCESO, TipoMovimiento.ENTRADA_REPROCESO);
        mapa.put(TipoMovimientoDetalle.SALIDA_REPROCESO, TipoMovimiento.SALIDA_REPROCESO);
        mapa.put(TipoMovimientoDetalle.SALIDA_ASEGURAMIENTO_CALIDAD, TipoMovimiento.SALIDA_ASEGURAMIENTO_CALIDAD);
        mapa.put(TipoMovimientoDetalle.ENTRADA_ASEGURAMIENTO_CALIDAD, TipoMovimiento.ENTRADA_ASEGURAMIENTO_CALIDAD);
    }

    public static TipoMovimiento obtenerTipoMovimiento(TipoMovimientoDetalle detalle) {
        return mapa.get(detalle);
    }
}
