package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;

import java.util.EnumMap;
import java.util.Map;

public class TipoMovimientoMapper {

    private static final Map<ClasificacionMovimientoInventario, TipoMovimiento> mapa = new EnumMap<>(ClasificacionMovimientoInventario.class);

    static {
        mapa.put(ClasificacionMovimientoInventario.RECEPCION_COMPRA, TipoMovimiento.RECEPCION_COMPRA);
        mapa.put(ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE, TipoMovimiento.ENTRADA_ASEGURAMIENTO_CALIDAD);
        mapa.put(ClasificacionMovimientoInventario.AJUSTE_POSITIVO, TipoMovimiento.AJUSTE_POSITIVO);
        mapa.put(ClasificacionMovimientoInventario.AJUSTE_NEGATIVO, TipoMovimiento.AJUSTE_NEGATIVO);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA_PRODUCCION);
        mapa.put(ClasificacionMovimientoInventario.ENTRADA_PRODUCCION, TipoMovimiento.ENTRADA_PRODUCCION);
        mapa.put(ClasificacionMovimientoInventario.TRANSFERENCIA_ENTRADA, TipoMovimiento.TRANSFERENCIA_ENTRADA);
        mapa.put(ClasificacionMovimientoInventario.TRANSFERENCIA_SALIDA, TipoMovimiento.TRANSFERENCIA_SALIDA);
        mapa.put(ClasificacionMovimientoInventario.DEVOLUCION_PROVEEDOR, TipoMovimiento.SALIDA_PRODUCCION);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_MUESTRA, TipoMovimiento.SALIDA_ASEGURAMIENTO_CALIDAD);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_OBSOLETO, TipoMovimiento.SALIDA_RETIRO_MERCADO);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_DONACION, TipoMovimiento.SALIDA_DONACION);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_RETIRO_MERCADO, TipoMovimiento.SALIDA_RETIRO_MERCADO);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_PERDIDA, TipoMovimiento.SALIDA_PERDIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_VENCIDO, TipoMovimiento.SALIDA_VENCIDO);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_MANTENIMIENTO, TipoMovimiento.SALIDA_MANTENIMIENTO);
        mapa.put(ClasificacionMovimientoInventario.ENTRADA_REPROCESO, TipoMovimiento.ENTRADA_REPROCESO);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_REPROCESO, TipoMovimiento.SALIDA_REPROCESO);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_ASEGURAMIENTO_CALIDAD, TipoMovimiento.SALIDA_ASEGURAMIENTO_CALIDAD);
        mapa.put(ClasificacionMovimientoInventario.ENTRADA_ASEGURAMIENTO_CALIDAD, TipoMovimiento.ENTRADA_ASEGURAMIENTO_CALIDAD);
    }

    public static TipoMovimiento obtenerTipoMovimiento(ClasificacionMovimientoInventario detalle) {
        return mapa.get(detalle);
    }
}
