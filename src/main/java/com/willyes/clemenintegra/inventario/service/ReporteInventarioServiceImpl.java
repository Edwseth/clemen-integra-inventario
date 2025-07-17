package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteInventarioServiceImpl implements ReporteInventarioService {

    private final MovimientoInventarioRepository movimientoRepo;
    private final OrdenCompraDetalleRepository ordenRepo;

    @Override
    public Workbook generarReporteAltaRotacion(LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
        List<Object[]> datos = movimientoRepo.conteoMovimientosDesc(inicio, fin);
        return crearExcelRotacion(datos, "Alta Rotacion");
    }

    @Override
    public Workbook generarReporteBajaRotacion(LocalDate fechaInicio, LocalDate fechaFin) {
        validarFechas(fechaInicio, fechaFin);
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
        List<Object[]> datos = movimientoRepo.conteoMovimientosAsc(inicio, fin);
        return crearExcelRotacion(datos, "Baja Rotacion");
    }

    @Override
    public Workbook generarReporteProductosMasCostosos(String categoria) {
        List<Object[]> datos = ordenRepo.productosMasCostosos(categoria);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Mas Costosos");
        String[] columnas = {"Producto", "SKU", "PrecioUnitario", "Proveedor", "TipoProducto"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }
        int idx = 1;
        for (Object[] obj : datos) {
            Row row = sheet.createRow(idx++);
            row.createCell(0).setCellValue(obj[0] != null ? obj[0].toString() : "");
            row.createCell(1).setCellValue(obj[1] != null ? obj[1].toString() : "");
            row.createCell(2).setCellValue(obj[2] != null ? Double.parseDouble(obj[2].toString()) : 0);
            row.createCell(3).setCellValue(obj[3] != null ? obj[3].toString() : "");
            row.createCell(4).setCellValue(obj[4] != null ? obj[4].toString() : "");
        }
        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }
        return workbook;
    }

    private Workbook crearExcelRotacion(List<Object[]> datos, String nombreHoja) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(nombreHoja);
        String[] columnas = {"Producto", "SKU", "CantidadMovimientos", "TipoProducto", "UnidadMedida"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }
        int idx = 1;
        for (Object[] obj : datos) {
            Row row = sheet.createRow(idx++);
            row.createCell(0).setCellValue(obj[0] != null ? obj[0].toString() : "");
            row.createCell(1).setCellValue(obj[1] != null ? obj[1].toString() : "");
            row.createCell(2).setCellValue(obj[2] != null ? Long.parseLong(obj[2].toString()) : 0);
            row.createCell(3).setCellValue(obj[3] != null ? obj[3].toString() : "");
            row.createCell(4).setCellValue(obj[4] != null ? obj[4].toString() : "");
        }
        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }
        return workbook;
    }

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("fechaFin debe ser posterior o igual a fechaInicio");
        }
    }
}
