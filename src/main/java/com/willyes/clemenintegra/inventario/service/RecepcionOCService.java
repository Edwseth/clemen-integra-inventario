package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.RecepcionOCResponseDTO;
import com.willyes.clemenintegra.inventario.model.RecepcionOC;

import java.time.LocalDate;
import java.util.List;

public interface RecepcionOCService {

    RecepcionOC findOrCreateCabecera(Integer ordenCompraId,
                                     Integer almacenDestinoId,
                                     Integer proveedorId,
                                     Long usuarioId,
                                     LocalDate fechaNegocio,
                                     String observaciones);

    RecepcionOCResponseDTO obtenerRecepcionPorCodigo(String codigo);

    List<RecepcionOCResponseDTO> listarRecepcionesPorOrden(Long ordenCompraId);
}
