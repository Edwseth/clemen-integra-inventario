package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.reportes.ConteoAjusteReporteDTO;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ReporteInventarioService {

    Workbook generarReporteAltaRotacion(LocalDate fechaInicio, LocalDate fechaFin);

    Workbook generarReporteBajaRotacion(LocalDate fechaInicio, LocalDate fechaFin);

    Workbook generarReporteProductosMasCostosos(String categoria);

    Workbook generarReporteTrazabilidadLote(String codigoLote);

    Workbook generarReporteProductosRetencionLiberacion(String estadoLote, LocalDate desde, LocalDate hasta);

    Workbook generarReporteNoConformidades(String tipo, String area, LocalDate desde, LocalDate hasta);

    Workbook generarReporteCapas(String estado, LocalDate desde, LocalDate hasta);

    Workbook generarReporteProductosVencidosExcel(Long productoId, Long almacenId);

    Page<ConteoAjusteReporteDTO> listarConteosAjuste(LocalDateTime fechaInicio,
                                                     LocalDateTime fechaFin,
                                                     Long almacenId,
                                                     Long productoId,
                                                     boolean soloConDiferencia,
                                                     Integer page,
                                                     Integer size,
                                                     String sortField,
                                                     String sortDir);

    Workbook generarExcelConteosAjuste(LocalDateTime fechaInicio,
                                       LocalDateTime fechaFin,
                                       Long almacenId,
                                       Long productoId,
                                       boolean soloConDiferencia);
}
