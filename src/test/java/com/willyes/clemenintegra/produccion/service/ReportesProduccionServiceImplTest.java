package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.ConsumoTeoricoRealItemDTO;
import com.willyes.clemenintegra.produccion.dto.ConsumoTeoricoRealResponseDTO;
import com.willyes.clemenintegra.produccion.dto.LoteTerminadoResumenDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportesProduccionServiceImplTest {

    @Mock
    private ConsumoTeoricoRealService consumoTeoricoRealService;
    @Mock
    private ReporteBatchRecordService reporteBatchRecordService;

    @InjectMocks
    private ReportesProduccionServiceImpl service;

    @Test
    @DisplayName("genera excel con datos de consumos y lotes")
    void generarExcel() {
        ConsumoTeoricoRealResponseDTO resumen = ConsumoTeoricoRealResponseDTO.builder()
                .items(List.of(ConsumoTeoricoRealItemDTO.builder()
                        .nombreProducto("Insumo 1")
                        .codigoSku("INS-1")
                        .unidad("KG")
                        .cantidadTeorica(BigDecimal.ONE)
                        .cantidadReal(BigDecimal.ONE)
                        .diferencia(BigDecimal.ZERO)
                        .porcentajeDesviacion(BigDecimal.ZERO)
                        .build()))
                .lotesTerminados(List.of(LoteTerminadoResumenDTO.builder()
                        .idLote(1L)
                        .codigoLote("L-01")
                        .estadoLote("DISPONIBLE")
                        .build()))
                .build();
        when(consumoTeoricoRealService.obtenerConsumo(5L)).thenReturn(resumen);

        byte[] bytes = service.generarBatchRecordExcel(5L);

        assertThat(bytes).isNotEmpty();
    }

    @Test
    @DisplayName("delegar pdf a servicio existente")
    void generarPdf() {
        when(reporteBatchRecordService.generarPdfBatchRecord(7L)).thenReturn(new byte[]{1, 2, 3});

        byte[] bytes = service.generarBatchRecordPdf(7L);

        assertThat(bytes).containsExactly(1, 2, 3);
    }
}

