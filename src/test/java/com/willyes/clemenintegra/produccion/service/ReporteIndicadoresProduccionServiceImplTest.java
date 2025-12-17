package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.AlertaOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReporteIndicadoresProduccionServiceImplTest {

    private final ReporteIndicadoresProduccionServiceImpl service = new ReporteIndicadoresProduccionServiceImpl();

    @Test
    @DisplayName("generarExcelIndicadores crea workbook con hojas KPIs y Alertas")
    void generarExcelIndicadores_creaHojas() throws Exception {
        IndicadoresProduccionResponseDTO dto = IndicadoresProduccionResponseDTO.builder()
                .totalOrdenesPeriodo(5)
                .ordenesEnTiempo(3)
                .ordenesRetrasadas(2)
                .porcentajeCumplimiento(60.0)
                .cantidadTotalPlanificada(new BigDecimal("100"))
                .cantidadTotalProducida(new BigDecimal("80"))
                .ordenesAbiertasConVencimientoVencido(1)
                .diasAlerta(4)
                .alertas(List.of(
                        AlertaOrdenProduccionDTO.builder()
                                .codigoOrden("OP-1")
                                .productoPrincipal("Producto A")
                                .fechaCompromiso(LocalDate.now())
                                .tipoAlerta("PROXIMA_VENCER")
                                .estado("EN_PROCESO")
                                .build()))
                .build();

        byte[] excel = service.generarExcelIndicadores(dto, LocalDate.now().minusDays(1), LocalDate.now());

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertThat(workbook.getNumberOfSheets()).isGreaterThanOrEqualTo(2);
            assertThat(workbook.getSheet("KPIs")).isNotNull();
            assertThat(workbook.getSheet("Alertas")).isNotNull();
        }
    }
}

