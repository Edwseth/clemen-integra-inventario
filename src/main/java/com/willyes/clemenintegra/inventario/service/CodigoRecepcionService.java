package com.willyes.clemenintegra.inventario.service;

import java.time.LocalDate;

public interface CodigoRecepcionService {

    String generarCodigo(LocalDate fechaNegocio);
}
