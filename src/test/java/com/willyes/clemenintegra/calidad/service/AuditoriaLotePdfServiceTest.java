package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaLotePdfServiceTest {

    @Test
    void generaPdfSinErrores() {
        AuditoriaLoteResponseDTO auditoria = AuditoriaLoteResponseDTO.builder()
                .codigoLote("LT-100")
                .nombreProducto("Producto A")
                .categoriaProducto("Categoria")
                .tipoAnalisisCalidad("FISICO")
                .estadoLote("LIBERADO")
                .fechaFabricacion(LocalDateTime.of(2024, 1, 10, 8, 0))
                .fechaVencimiento(LocalDateTime.of(2025, 1, 10, 8, 0))
                .stockLote(new BigDecimal("120.5"))
                .nombreAlmacenActual("Almacen Central")
                .ubicacionAlmacenActual("Rack A1")
                .estadoCalidad(EstadoCalidadLoteResponseDTO.builder()
                        .estadoLote("LIBERADO")
                        .retencionActiva(false)
                        .build())
                .incidentes(List.of())
                .movimientos(List.of())
                .build();

        AuditoriaLotePdfService service = new AuditoriaLotePdfService();

        byte[] pdf = service.generarPdf(auditoria);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);
    }
}
