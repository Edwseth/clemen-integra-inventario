package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.ComponenteImpactoResponseDTO;

public interface ComponenteImpactoService {
    ComponenteImpactoResponseDTO obtenerImpactoPorProductoId(Long productoId);
}
