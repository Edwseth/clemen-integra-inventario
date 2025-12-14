package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ConsumoTeoricoRealResponseDTO;
import com.willyes.clemenintegra.produccion.dto.LoteTerminadoResumenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportesProduccionServiceImpl implements ReportesProduccionService {

    private final ConsumoTeoricoRealService consumoTeoricoRealService;
    private final ReporteBatchRecordService reporteBatchRecordService;

    @Override
    public byte[] generarBatchRecordExcel(Long ordenProduccionId) {
        ConsumoTeoricoRealResponseDTO resumen = consumoTeoricoRealService.obtenerConsumo(ordenProduccionId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            construirHojaConsumos(workbook, resumen);
            construirHojaLotes(workbook, resumen);
            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel de batch record para OP {}", ordenProduccionId, e);
            throw new RuntimeException("No fue posible generar el Excel de batch record", e);
        }
    }

    @Override
    public byte[] generarBatchRecordPdf(Long ordenProduccionId) {
        return reporteBatchRecordService.generarPdfBatchRecord(ordenProduccionId);
    }

    private void construirHojaConsumos(Workbook workbook, ConsumoTeoricoRealResponseDTO resumen) {
        Sheet sheet = workbook.createSheet("Consumos");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Producto");
        header.createCell(1).setCellValue("SKU");
        header.createCell(2).setCellValue("Unidad");
        header.createCell(3).setCellValue("Cantidad teórica");
        header.createCell(4).setCellValue("Cantidad real");
        header.createCell(5).setCellValue("Diferencia");
        header.createCell(6).setCellValue("% Desviación");

        int rowIdx = 1;
        for (var item : resumen.getItems()) {
            Row row = sheet.createRow(rowIdx++);
            crearCelda(row, 0, item.getNombreProducto());
            crearCelda(row, 1, item.getCodigoSku());
            crearCelda(row, 2, item.getUnidad());
            crearCelda(row, 3, item.getCantidadTeorica());
            crearCelda(row, 4, item.getCantidadReal());
            crearCelda(row, 5, item.getDiferencia());
            crearCelda(row, 6, item.getPorcentajeDesviacion());
        }
        for (int i = 0; i <= 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void construirHojaLotes(Workbook workbook, ConsumoTeoricoRealResponseDTO resumen) {
        Sheet sheet = workbook.createSheet("Lotes terminados");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID Lote");
        header.createCell(1).setCellValue("Código Lote");
        header.createCell(2).setCellValue("Estado");
        header.createCell(3).setCellValue("Almacén");
        header.createCell(4).setCellValue("Stock");
        header.createCell(5).setCellValue("Vencimiento");

        int rowIdx = 1;
        for (LoteTerminadoResumenDTO lote : resumen.getLotesTerminados()) {
            Row row = sheet.createRow(rowIdx++);
            crearCelda(row, 0, lote.getIdLote());
            crearCelda(row, 1, lote.getCodigoLote());
            crearCelda(row, 2, lote.getEstadoLote());
            crearCelda(row, 3, lote.getAlmacenNombre());
            crearCelda(row, 4, lote.getStockLote());
            crearCelda(row, 5, lote.getFechaVencimiento());
        }
        for (int i = 0; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void crearCelda(Row row, int idx, Object value) {
        Cell cell = row.createCell(idx);
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}

