package com.willyes.clemenintegra.planeacion.service;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MrpReporteServiceTest {

    @Mock
    private CorridaMrpRepository corridaMrpRepository;

    @InjectMocks
    private MrpReporteService mrpReporteService;

    private CorridaMrp corrida;

    @BeforeEach
    void setUp() {
        CategoriaProducto categoria = CategoriaProducto.builder()
                .id(1L)
                .nombre("Materia Prima")
                .build();

        Producto producto = Producto.builder()
                .id(1)
                .codigoSku("MAT-001")
                .nombre("Ácido cítrico")
                .categoriaProducto(categoria)
                .build();

        DetalleCorridaMrp detalle = DetalleCorridaMrp.builder()
                .id(1L)
                .producto(producto)
                .requerimientoBruto(BigDecimal.valueOf(100))
                .inventarioDisponible(BigDecimal.valueOf(20))
                .requerimientoNeto(BigDecimal.valueOf(80))
                .build();

        SugerenciaAbastecimiento sugerencia = SugerenciaAbastecimiento.builder()
                .id(5L)
                .detalleCorrida(detalle)
                .tipo(TipoSugerenciaAbastecimiento.COMPRA)
                .cantidadSugerida(BigDecimal.valueOf(80))
                .fechaNecesidad(LocalDate.now())
                .estado(null)
                .build();
        detalle.setSugerencia(sugerencia);

        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(3L)
                .semanaInicio(LocalDate.now())
                .semanaFin(LocalDate.now().plusDays(7))
                .build();

        corrida = CorridaMrp.builder()
                .id(10L)
                .planProduccionSemanal(plan)
                .fechaEjecucion(LocalDateTime.now())
                .detalles(List.of(detalle))
                .build();
    }

    @Test
    void generarExcelCorridaDevuelveContenidoConFilaDatos() throws Exception {
        when(corridaMrpRepository.findWithDetallesById(anyLong())).thenReturn(Optional.of(corrida));

        byte[] excel = mrpReporteService.generarExcelCorrida(10L);

        assertNotNull(excel);
        assertNotEquals(0, excel.length);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = findHeaderRow(sheet);
            Row dataRow = sheet.getRow(header.getRowNum() + 1);

            assertNotNull(dataRow);
            assertEquals("MAT-001", dataRow.getCell(0).getStringCellValue());
            assertEquals("Ácido cítrico", dataRow.getCell(1).getStringCellValue());
            assertEquals("Materia Prima", dataRow.getCell(2).getStringCellValue());
            assertEquals("OC", dataRow.getCell(6).getStringCellValue());
        }
    }

    @Test
    void generarPdfCorridaDevuelveContenidoConDatos() throws Exception {
        when(corridaMrpRepository.findWithDetallesById(anyLong())).thenReturn(Optional.of(corrida));

        byte[] pdf = mrpReporteService.generarPdfCorrida(10L);

        assertNotNull(pdf);
        assertNotEquals(0, pdf.length);

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase();
            String flattened = normalized.replaceAll("\\s+", "");
            assertTrue(flattened.contains("mat-001"));
            assertTrue(flattened.contains("acidocitrico"));
            assertTrue(flattened.contains("materiaprima"));
            assertTrue(flattened.contains("oc"));
        }
    }

    private Row findHeaderRow(Sheet sheet) {
        for (Row row : sheet) {
            if (row.getCell(0) != null && "Código insumo".equals(row.getCell(0).getStringCellValue())) {
                return row;
            }
        }
        throw new IllegalStateException("No se encontró la fila de encabezado en el Excel generado");
    }
}
