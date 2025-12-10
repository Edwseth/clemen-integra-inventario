package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;

public interface PlantillaAnalisisMicroService {
    PlantillaAnalisisMicroDTO obtenerPorProducto(Long productoId);
}

