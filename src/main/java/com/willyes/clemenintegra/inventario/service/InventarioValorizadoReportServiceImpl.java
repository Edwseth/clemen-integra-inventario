package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.reportes.InventarioValorizadoRowDTO;
import com.willyes.clemenintegra.inventario.repository.InventarioValorizadoRepository;
import com.willyes.clemenintegra.inventario.repository.InventarioValorizadoRowProjection;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioValorizadoReportServiceImpl implements InventarioValorizadoReportService {

    private final InventarioValorizadoRepository inventarioValorizadoRepository;

    @Override
    public Page<InventarioValorizadoRowDTO> listar(Pageable pageable) {
        return inventarioValorizadoRepository.findInventarioValorizado(pageable).map(this::toDto);
    }

    @Override
    public Workbook generarExcel() {
        List<InventarioValorizadoRowDTO> filas = inventarioValorizadoRepository
                .findInventarioValorizado(Pageable.unpaged())
                .map(this::toDto)
                .getContent();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventario valorizado");

        String[] headers = {
                "Sku", "Nombre", "UDM", "Lote", "Vence", "Ubicación/Almacén", "Stock",
                "Costo_unitario_material", "Valor_total"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        DataFormat dataFormat = workbook.createDataFormat();
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(dataFormat.getFormat("#,##0.000000"));

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (InventarioValorizadoRowDTO fila : filas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nvl(fila.sku()));
            row.createCell(1).setCellValue(nvl(fila.nombre()));
            row.createCell(2).setCellValue(nvl(fila.udm()));
            row.createCell(3).setCellValue(nvl(fila.lote()));
            row.createCell(4).setCellValue(fila.vence() == null ? "" : fila.vence().toString());
            row.createCell(5).setCellValue(nvl(fila.ubicacionAlmacen()));

            Cell stockCell = row.createCell(6);
            stockCell.setCellValue(toDouble(fila.stock()));
            stockCell.setCellStyle(numberStyle);

            Cell costoCell = row.createCell(7);
            costoCell.setCellValue(toDouble(fila.costoUnitarioMaterial()));
            costoCell.setCellStyle(numberStyle);

            Cell totalCell = row.createCell(8);
            totalCell.setCellValue(toDouble(fila.valorTotal()));
            totalCell.setCellStyle(numberStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    private InventarioValorizadoRowDTO toDto(InventarioValorizadoRowProjection p) {
        BigDecimal stock = nvl(p.getStock());
        BigDecimal costo = nvl(p.getCostoUnitarioMaterial());
        BigDecimal valor = stock.multiply(costo).setScale(6, RoundingMode.HALF_UP);
        return new InventarioValorizadoRowDTO(
                p.getSku(),
                p.getNombre(),
                p.getUdm(),
                p.getLote(),
                p.getVence(),
                p.getUbicacionAlmacen(),
                stock,
                costo,
                valor
        );
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP) : value.setScale(6, RoundingMode.HALF_UP);
    }

    private double toDouble(BigDecimal value) {
        return nvl(value).doubleValue();
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
