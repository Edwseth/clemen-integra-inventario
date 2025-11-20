package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;

import java.util.List;

public interface ReporteOrdenProduccionService {

    byte[] generarExcelOrdenesProduccion(List<OrdenProduccion> ordenes);

    byte[] generarPdfOrdenesProduccion(List<OrdenProduccion> ordenes);
}
