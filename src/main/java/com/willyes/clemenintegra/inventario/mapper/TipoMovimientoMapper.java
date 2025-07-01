package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;

import java.util.EnumMap;
import java.util.Map;

public class TipoMovimientoMapper {

    private static final Map<ClasificacionMovimientoInventario, TipoMovimiento> mapa = new EnumMap<>(ClasificacionMovimientoInventario.class);

    static {
        mapa.put(ClasificacionMovimientoInventario.RECEPCION_COMPRA, TipoMovimiento.RECEPCION);
        mapa.put(ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE, TipoMovimiento.RECEPCION);

        mapa.put(ClasificacionMovimientoInventario.AJUSTE_POSITIVO, TipoMovimiento.AJUSTE);
        mapa.put(ClasificacionMovimientoInventario.AJUSTE_NEGATIVO, TipoMovimiento.AJUSTE);

        mapa.put(ClasificacionMovimientoInventario.ENTRADA_PRODUCCION, TipoMovimiento.ENTRADA);
        mapa.put(ClasificacionMovimientoInventario.ENTRADA_ASEGURAMIENTO_CALIDAD, TipoMovimiento.ENTRADA);
        mapa.put(ClasificacionMovimientoInventario.ENTRADA_REPROCESO, TipoMovimiento.ENTRADA);
        mapa.put(ClasificacionMovimientoInventario.INGRESO_PT_DESDE_PRODUCCION, TipoMovimiento.ENTRADA);

        mapa.put(ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_VENCIDO, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_MUESTRA_GRATIS, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_MUESTRA_CALIDAD, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_OBSOLETO, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_DONACION, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_PERDIDA, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_MANTENIMIENTO, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_AVERIADO, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_RETIRO_MERCADO, TipoMovimiento.SALIDA);
        mapa.put(ClasificacionMovimientoInventario.SALIDA_REPROCESO, TipoMovimiento.SALIDA);

        mapa.put(ClasificacionMovimientoInventario.DEVOLUCION_PROVEEDOR, TipoMovimiento.DEVOLUCION);
        mapa.put(ClasificacionMovimientoInventario.DEVOLUCION_PRODUCCION_BODEGA, TipoMovimiento.DEVOLUCION);

        mapa.put(ClasificacionMovimientoInventario.TRANSFERENCIA_BODEGA_PRODUCCION, TipoMovimiento.TRANSFERENCIA);
    }

    public static TipoMovimiento obtenerTipoMovimiento(ClasificacionMovimientoInventario detalle) {
        return mapa.get(detalle);
    }
}

