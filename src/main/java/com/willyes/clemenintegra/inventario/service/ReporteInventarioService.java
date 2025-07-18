package com.willyes.clemenintegra.inventario.service;

import org.apache.poi.ss.usermodel.Workbook;

import java.time.LocalDate;

public interface ReporteInventarioService {

    Workbook generarReporteAltaRotacion(LocalDate fechaInicio, LocalDate fechaFin);

    Workbook generarReporteBajaRotacion(LocalDate fechaInicio, LocalDate fechaFin);

    Workbook generarReporteProductosMasCostosos(String categoria);

    Workbook generarReporteTrazabilidadLote(String codigoLote);

    Workbook generarReporteProductosRetencionLiberacion(String estadoLote, LocalDate desde, LocalDate hasta);

    Workbook generarReporteNoConformidades(String tipo, String area, LocalDate desde, LocalDate hasta);

    Workbook generarReporteCapas(String estado, LocalDate desde, LocalDate hasta);
}
