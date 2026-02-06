package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.reportes.ReporteComprasRowDTO;

import java.time.LocalDate;
import java.util.List;

public interface ReporteComprasService {

    List<ReporteComprasRowDTO> generar(LocalDate desde, LocalDate hasta);

    byte[] exportarExcel(LocalDate desde, LocalDate hasta);
}
