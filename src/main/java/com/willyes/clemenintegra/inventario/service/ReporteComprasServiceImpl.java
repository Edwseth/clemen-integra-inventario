package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.reportes.ReporteComprasRowDTO;
import com.willyes.clemenintegra.inventario.repository.ReporteComprasRepository;
import com.willyes.clemenintegra.inventario.repository.ReporteComprasRowProjection;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteComprasServiceImpl implements ReporteComprasService {

    private static final DateTimeFormatter FECHA_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FECHA_HORA_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReporteComprasRepository reporteComprasRepository;

    @Override
    public List<ReporteComprasRowDTO> generar(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        LocalDateTime fechaDesde = desde.atStartOfDay();
        LocalDateTime fechaHasta = hasta.atTime(LocalTime.MAX);

        return reporteComprasRepository.obtenerReporte(fechaDesde, fechaHasta)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public byte[] exportarExcel(LocalDate desde, LocalDate hasta) {
        List<ReporteComprasRowDTO> filas = generar(desde, hasta);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("ReporteCompras");
            String[] headers = {
                    "OC", "Estado", "Código", "Nombre", "UDM", "Cantidad", "Fecha OC", "Fecha pactada", "Fecha recepción",
                    "Proveedor", "Condiciones de pago", "Precio unitario", "IVA"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (ReporteComprasRowDTO fila : filas) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(valorTexto(fila.getOcCodigo()));
                row.createCell(1).setCellValue(valorTexto(fila.getEstado()));
                row.createCell(2).setCellValue(valorTexto(fila.getProductoCodigo()));
                row.createCell(3).setCellValue(valorTexto(fila.getProductoNombre()));
                row.createCell(4).setCellValue(valorTexto(fila.getUdm()));
                row.createCell(5).setCellValue(valorDecimal(fila.getCantidad()));
                row.createCell(6).setCellValue(fila.getFechaOc() != null ? fila.getFechaOc().format(FECHA_HORA_FORMAT) : "");
                row.createCell(7).setCellValue(fila.getFechaPactada() != null ? fila.getFechaPactada().format(FECHA_FORMAT) : "");
                row.createCell(8).setCellValue(fila.getFechaRecepcion() != null ? fila.getFechaRecepcion().format(FECHA_FORMAT) : "");
                row.createCell(9).setCellValue(valorTexto(fila.getProveedorNombre()));
                row.createCell(10).setCellValue(valorTexto(fila.getCondicionesPago()));
                row.createCell(11).setCellValue(valorDecimal(fila.getPrecioUnitario()));
                row.createCell(12).setCellValue(valorDecimal(fila.getIva()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible generar el archivo Excel del reporte de compras", e);
        }
    }

    private ReporteComprasRowDTO toDto(ReporteComprasRowProjection projection) {
        return ReporteComprasRowDTO.builder()
                .ocCodigo(projection.getOcCodigo())
                .estado(projection.getEstado())
                .productoCodigo(projection.getProductoCodigo())
                .productoNombre(projection.getProductoNombre())
                .udm(projection.getUdm())
                .cantidad(projection.getCantidad())
                .fechaOc(projection.getFechaOc())
                .fechaPactada(projection.getFechaPactada())
                .fechaRecepcion(projection.getFechaRecepcion())
                .proveedorNombre(projection.getProveedorNombre())
                .condicionesPago(projection.getCondicionesPago())
                .precioUnitario(projection.getPrecioUnitario())
                .iva(projection.getIva())
                .build();
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Los parámetros desde y hasta son obligatorios");
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a la fecha desde");
        }
    }

    private String valorTexto(String valor) {
        return valor != null ? valor : "";
    }

    private String valorDecimal(BigDecimal valor) {
        return valor != null ? valor.toPlainString() : "";
    }
}
