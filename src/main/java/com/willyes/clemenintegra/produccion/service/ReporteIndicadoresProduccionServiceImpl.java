package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.AlertaOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ReporteIndicadoresProduccionServiceImpl implements ReporteIndicadoresProduccionService {

    @Override
    public byte[] generarExcelIndicadores(IndicadoresProduccionResponseDTO indicadores,
                                          LocalDate fechaInicio,
                                          LocalDate fechaFin) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            construirHojaKpis(workbook, indicadores, fechaInicio, fechaFin);
            construirHojaAlertas(workbook, indicadores.getAlertas());
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel de indicadores de producción", e);
            throw new IllegalStateException("No se pudo generar el Excel de indicadores de producción", e);
        }
    }

    private void construirHojaKpis(Workbook workbook,
                                   IndicadoresProduccionResponseDTO indicadores,
                                   LocalDate fechaInicio,
                                   LocalDate fechaFin) {
        Sheet sheet = workbook.createSheet("KPIs");
        int rowIdx = 0;
        rowIdx = crearFila(sheet, rowIdx, "Rango consultado", fechaInicio + " a " + fechaFin);
        rowIdx = crearFila(sheet, rowIdx, "Total órdenes periodo", indicadores.getTotalOrdenesPeriodo());
        rowIdx = crearFila(sheet, rowIdx, "Órdenes en tiempo", indicadores.getOrdenesEnTiempo());
        rowIdx = crearFila(sheet, rowIdx, "Órdenes retrasadas", indicadores.getOrdenesRetrasadas());
        rowIdx = crearFila(sheet, rowIdx, "% cumplimiento", indicadores.getPorcentajeCumplimiento());
        rowIdx = crearFila(sheet, rowIdx, "Cantidad total planificada", indicadores.getCantidadTotalPlanificada());
        rowIdx = crearFila(sheet, rowIdx, "Cantidad total producida", indicadores.getCantidadTotalProducida());
        rowIdx = crearFila(sheet, rowIdx, "Órdenes abiertas vencidas", indicadores.getOrdenesAbiertasConVencimientoVencido());
        if (indicadores.getDiasAlerta() != null) {
            crearFila(sheet, rowIdx, "Ventana alertas (días)", indicadores.getDiasAlerta());
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void construirHojaAlertas(Workbook workbook, List<AlertaOrdenProduccionDTO> alertas) {
        Sheet sheet = workbook.createSheet("Alertas");
        Row header = sheet.createRow(0);
        String[] headers = {"Código OP", "Producto", "Fecha compromiso", "Tipo alerta", "Estado"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        int rowIdx = 1;
        if (alertas != null) {
            for (AlertaOrdenProduccionDTO alerta : alertas) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(valueOf(alerta.getCodigoOrden()));
                row.createCell(1).setCellValue(valueOf(alerta.getProductoPrincipal()));
                row.createCell(2).setCellValue(alerta.getFechaCompromiso() != null ? alerta.getFechaCompromiso().toString() : "");
                row.createCell(3).setCellValue(valueOf(alerta.getTipoAlerta()));
                row.createCell(4).setCellValue(valueOf(alerta.getEstado()));
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private int crearFila(Sheet sheet, int rowIdx, String clave, Object valor) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(clave);
        if (valor instanceof Number number) {
            row.createCell(1).setCellValue(number.doubleValue());
        } else {
            row.createCell(1).setCellValue(valueOf(valor));
        }
        return rowIdx + 1;
    }

    private String valueOf(Object valor) {
        return valor == null ? "" : valor.toString();
    }
}

