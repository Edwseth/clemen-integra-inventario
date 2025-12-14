package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ConsumoTeoricoRealResponseDTO;

public interface ConsumoTeoricoRealService {

    ConsumoTeoricoRealResponseDTO obtenerConsumo(Long ordenProduccionId);
}

