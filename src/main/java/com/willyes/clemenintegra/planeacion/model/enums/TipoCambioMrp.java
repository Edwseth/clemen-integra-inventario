package com.willyes.clemenintegra.planeacion.model.enums;

/**
 * Representa el tipo de variación del requerimiento neto de un insumo entre la corrida MRP actual
 * y la última corrida completada del mismo plan de producción semanal.
 */
public enum TipoCambioMrp {
    NUEVO,
    AUMENTO,
    REDUCCION,
    SIN_CAMBIO
}
