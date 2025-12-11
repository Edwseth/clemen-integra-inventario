package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.inventario.model.Producto;

/**
 * Utilidades para determinar los análisis de calidad requeridos por producto.
 * Siempre basadas en las banderas independientes introducidas para cada disciplina.
 */
public final class AnalisisCalidadHelper {

    private AnalisisCalidadHelper() {
    }

    public static boolean requiereFisico(Producto producto) {
        return producto != null && producto.isRequiereAnalisisFisico();
    }

    public static boolean requiereQuimico(Producto producto) {
        return producto != null && producto.isRequiereAnalisisQuimico();
    }

    public static boolean requiereMicro(Producto producto) {
        return producto != null && producto.isRequiereAnalisisMicrobiologico();
    }
}
