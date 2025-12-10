package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;

public interface PlantillaAnalisisMicroService {
    /**
     * Backend como fuente de verdad: determina si el producto requiere análisis microbiológico
     * y, de ser así, devuelve la plantilla asociada con sus parámetros.
     */
    PlantillaAnalisisMicroDTO obtenerPorProducto(Long productoId);
}

