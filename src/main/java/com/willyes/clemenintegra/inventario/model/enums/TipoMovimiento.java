package com.willyes.clemenintegra.inventario.model.enums;

public enum TipoMovimiento {
    AJUSTE,         // Para correcciones manuales del stock
    DEVOLUCION,     // Para devoluciones internas o a proveedor
    ENTRADA,        // Para ingreso de producto terminado desde producción
    RECEPCION,      // Para ingreso con orden de compra
    SALIDA,         // Exclusiva para egresos de MP/ME hacia pre-bodega
    TRANSFERENCIA   // Movimientos entre almacenes internos (no afecta stock)
}





