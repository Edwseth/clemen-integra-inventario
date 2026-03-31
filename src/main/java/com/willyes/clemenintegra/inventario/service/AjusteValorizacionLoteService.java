package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteRequestDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteResponseDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.ValorizacionElegibilidadResponseDTO;

public interface AjusteValorizacionLoteService {

    ValorizacionElegibilidadResponseDTO evaluarElegibilidad(Long loteId);

    AjusteValorizacionLoteResponseDTO ajustar(Long loteId,
                                              AjusteValorizacionLoteRequestDTO request,
                                              String idempotencyKey);
}
