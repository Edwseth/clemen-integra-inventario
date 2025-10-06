package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.RecepcionOC;

import java.time.LocalDate;

public interface RecepcionOCService {

    RecepcionOC findOrCreateCabecera(Integer ordenCompraId,
                                     Integer almacenDestinoId,
                                     Integer proveedorId,
                                     Long usuarioId,
                                     LocalDate fechaNegocio,
                                     String observaciones);
}
