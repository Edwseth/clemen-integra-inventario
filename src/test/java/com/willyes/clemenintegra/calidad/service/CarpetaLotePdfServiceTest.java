package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarpetaLotePdfServiceTest {

    @Mock
    private AuditoriaLoteService auditoriaLoteService;

    @Mock
    private EvaluacionCalidadRepository evaluacionCalidadRepository;

    @Test
    void generaCarpetaSinErrores() {
        AuditoriaLoteResponseDTO auditoria = AuditoriaLoteResponseDTO.builder()
                .loteId(1L)
                .codigoLote("LT-200")
                .nombreProducto("Producto B")
                .estadoLote("CUARENTENA")
                .fechaFabricacion(LocalDateTime.of(2024, 2, 10, 9, 0))
                .estadoCalidad(EstadoCalidadLoteResponseDTO.builder()
                        .estadoLote("CUARENTENA")
                        .retencionActiva(true)
                        .build())
                .incidentes(List.of())
                .movimientos(List.of())
                .build();

        when(auditoriaLoteService.obtenerAuditoriaDeLote(1L)).thenReturn(auditoria);
        when(evaluacionCalidadRepository.findByLoteProductoId(1L)).thenReturn(List.of());

        CarpetaLotePdfService service = new CarpetaLotePdfService(auditoriaLoteService, evaluacionCalidadRepository);

        byte[] pdf = service.generarCarpeta(1L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);
    }
}
