package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Primary
@Service
public class ReporteOrdenProduccionServiceStub implements ReporteOrdenProduccionService {
    @Override
    public byte[] generarExcelOrdenesProduccion(List<OrdenProduccion> ordenes) {
        return new byte[0];
    }

    @Override
    public byte[] generarPdfOrdenesProduccion(List<OrdenProduccion> ordenes) {
        return new byte[0];
    }
}
