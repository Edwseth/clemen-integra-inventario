package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.AlertaOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduccionIndicadoresServiceImplTest {

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;

    @InjectMocks
    private ProduccionIndicadoresServiceImpl service;

    private LocalDateTime ahora;

    @BeforeEach
    void setUp() {
        ahora = LocalDateTime.now();
    }

    @Test
    @DisplayName("calcularIndicadores resume ordenes en tiempo y retrasadas con cantidades")
    void calcularIndicadores_calculaKPIs() {
        OrdenProduccion enTiempo = OrdenProduccion.builder()
                .id(1L)
                .codigoOrden("OP-EN-TIEMPO")
                .fechaFin(ahora.plusHours(1))
                .fechaCierre(ahora)
                .estado(EstadoProduccion.FINALIZADA)
                .cantidadProgramada(new BigDecimal("319782"))
                .cantidadProducidaAcumulada(new BigDecimal("253151"))
                .build();

        when(ordenProduccionRepository.findByFechaFinBetween(any(), any())).thenReturn(
                java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(enTiempo),
                        java.util.stream.Stream.generate(() -> OrdenProduccion.builder()
                                        .estado(EstadoProduccion.EN_PROCESO)
                                        .fechaFin(ahora.minusDays(1))
                                        .cantidadProgramada(BigDecimal.ZERO)
                                        .cantidadProducida(BigDecimal.ZERO)
                                        .build())
                                .limit(200)
                ).toList()
        );

        IndicadoresProduccionResponseDTO resultado = service.calcularIndicadores(
                LocalDate.now().minusDays(3), LocalDate.now());

        assertThat(resultado.getTotalOrdenesPeriodo()).isEqualTo(201);
        assertThat(resultado.getOrdenesEnTiempo()).isEqualTo(1);
        assertThat(resultado.getOrdenesRetrasadas()).isEqualTo(200);
        assertThat(resultado.getOrdenesAbiertasConVencimientoVencido()).isEqualTo(200);
        assertThat(resultado.getCantidadTotalPlanificada()).isEqualByComparingTo("319782.00");
        assertThat(resultado.getCantidadTotalProducida()).isEqualByComparingTo("253151.00");
        assertThat(resultado.getPorcentajeCumplimiento()).isCloseTo(0.497512, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(resultado.getPorcentajeOrdenesEnTiempo()).isCloseTo(0.497512, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(resultado.getPorcentajeCumplimientoProduccion()).isCloseTo(79.161369, org.assertj.core.data.Offset.offset(0.000001));
    }

    @Test
    @DisplayName("calcularIndicadores retorna cero cuando no hay ordenes")
    void calcularIndicadores_sinOrdenes() {
        when(ordenProduccionRepository.findByFechaFinBetween(any(), any())).thenReturn(List.of());

        IndicadoresProduccionResponseDTO resultado = service.calcularIndicadores(
                LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(resultado.getTotalOrdenesPeriodo()).isZero();
        assertThat(resultado.getPorcentajeCumplimiento()).isZero();
        assertThat(resultado.getPorcentajeOrdenesEnTiempo()).isZero();
        assertThat(resultado.getPorcentajeCumplimientoProduccion()).isZero();
        assertThat(resultado.getCantidadTotalPlanificada()).isEqualByComparingTo("0.00");
        assertThat(resultado.getCantidadTotalProducida()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("calcularIndicadores calcula porcentajes esperados para 40 en tiempo y 200 retrasadas")
    void calcularIndicadores_porcentajesPuntualidadYProduccion() {
        List<OrdenProduccion> ordenes = new java.util.ArrayList<>();
        for (int i = 0; i < 39; i++) {
            ordenes.add(OrdenProduccion.builder()
                    .estado(EstadoProduccion.FINALIZADA)
                    .fechaFin(ahora.plusDays(1))
                    .fechaCierre(ahora)
                    .cantidadProgramada(BigDecimal.ZERO)
                    .cantidadProducida(BigDecimal.ZERO)
                    .build());
        }
        for (int i = 0; i < 200; i++) {
            ordenes.add(OrdenProduccion.builder()
                    .estado(EstadoProduccion.EN_PROCESO)
                    .fechaFin(ahora.minusDays(1))
                    .cantidadProgramada(BigDecimal.ZERO)
                    .cantidadProducida(BigDecimal.ZERO)
                    .build());
        }
        ordenes.add(OrdenProduccion.builder()
                .estado(EstadoProduccion.FINALIZADA)
                .fechaFin(ahora.plusDays(2))
                .fechaCierre(ahora)
                .cantidadProgramada(new BigDecimal("319782"))
                .cantidadProducidaAcumulada(new BigDecimal("253151"))
                .build());

        when(ordenProduccionRepository.findByFechaFinBetween(any(), any())).thenReturn(ordenes);

        IndicadoresProduccionResponseDTO resultado = service.calcularIndicadores(
                LocalDate.now().minusDays(3), LocalDate.now().plusDays(3));

        assertThat(resultado.getOrdenesEnTiempo()).isEqualTo(40);
        assertThat(resultado.getOrdenesRetrasadas()).isEqualTo(200);
        assertThat(resultado.getPorcentajeCumplimiento()).isCloseTo(16.666666, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(resultado.getPorcentajeOrdenesEnTiempo()).isCloseTo(16.666666, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(resultado.getPorcentajeCumplimientoProduccion()).isCloseTo(79.161369, org.assertj.core.data.Offset.offset(0.000001));
    }

    @Test
    @DisplayName("obtenerOrdenesConAlertas identifica proximas y retrasadas")
    void obtenerOrdenesConAlertas_devuelveLista() {
        LocalDate referencia = LocalDate.now();
        OrdenProduccion proxima = OrdenProduccion.builder()
                .id(10L)
                .codigoOrden("OP-PROX")
                .estado(EstadoProduccion.EN_PROCESO)
                .fechaFin(referencia.plusDays(2).atStartOfDay())
                .build();

        OrdenProduccion retrasada = OrdenProduccion.builder()
                .id(11L)
                .codigoOrden("OP-RET")
                .estado(EstadoProduccion.EN_PROCESO)
                .fechaFin(referencia.minusDays(1).atStartOfDay())
                .build();

        when(ordenProduccionRepository.findByEstadoNotInAndFechaFinBetween(any(), any(), any()))
                .thenReturn(List.of(proxima));
        when(ordenProduccionRepository.findByEstadoNotInAndFechaFinBefore(any(), any()))
                .thenReturn(List.of(retrasada));

        List<AlertaOrdenProduccionDTO> alertas = service.obtenerOrdenesConAlertas(referencia, 3);

        assertThat(alertas).hasSize(2);
        assertThat(alertas)
                .extracting(AlertaOrdenProduccionDTO::getTipoAlerta)
                .containsExactlyInAnyOrder("PROXIMA_VENCER", "RETRASADA");
    }
}
