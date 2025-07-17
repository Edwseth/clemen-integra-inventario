package com.willyes.clemenintegra.inventario.service;

import org.apache.poi.ss.usermodel.Workbook;

import java.time.LocalDate;

public interface ReporteInventarioService {

    Workbook generarReporteAltaRotacion(LocalDate fechaInicio, LocalDate fechaFin);

    Workbook generarReporteBajaRotacion(LocalDate fechaInicio, LocalDate fechaFin);

    Workbook generarReporteProductosMasCostosos(String categoria);
}
