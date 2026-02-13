package com.willyes.clemenintegra.inventario.regularizacion.service;

import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadResponseDTO;
import com.willyes.clemenintegra.shared.model.Usuario;

public interface RegularizacionTrazabilidadService {

    RegularizacionTrazabilidadResponseDTO regularizarPorOP(RegularizacionTrazabilidadRequestDTO request,
                                                           String idempotencyKey,
                                                           Usuario usuarioAuth);
}
