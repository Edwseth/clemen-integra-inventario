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
                .codigoOrden("OP1")
                .fechaFin(ahora.plusHours(1))
                .fechaCierre(ahora)
                .estado(EstadoProduccion.FINALIZADA)
                .cantidadProgramada(new BigDecimal("10"))
                .cantidadProducidaAcumulada(new BigDecimal("12"))
                .build();

        OrdenProduccion cerradaRetrasada = OrdenProduccion.builder()
                .id(2L)
                .codigoOrden("OP2")
                .fechaFin(ahora.minusDays(1))
                .fechaCierre(ahora)
                .estado(EstadoProduccion.FINALIZADA)
                .cantidadProgramada(new BigDecimal("5"))
                .cantidadProducida(new BigDecimal("4"))
                .build();

        OrdenProduccion abiertaRetrasada = OrdenProduccion.builder()
                .id(3L)
                .codigoOrden("OP3")
                .fechaFin(ahora.minusDays(2))
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(new BigDecimal("8"))
                .build();

        when(ordenProduccionRepository.findByFechaFinBetween(any(), any()))
                .thenReturn(List.of(enTiempo, cerradaRetrasada, abiertaRetrasada));

        IndicadoresProduccionResponseDTO resultado = service.calcularIndicadores(
                LocalDate.now().minusDays(3), LocalDate.now());

        assertThat(resultado.getTotalOrdenesPeriodo()).isEqualTo(3);
        assertThat(resultado.getOrdenesEnTiempo()).isEqualTo(1);
        assertThat(resultado.getOrdenesRetrasadas()).isEqualTo(2);
        assertThat(resultado.getOrdenesAbiertasConVencimientoVencido()).isEqualTo(1);
        assertThat(resultado.getCantidadTotalPlanificada()).isEqualByComparingTo("23.00");
        assertThat(resultado.getCantidadTotalProducida()).isEqualByComparingTo("16.00");
        assertThat(resultado.getPorcentajeCumplimiento()).isGreaterThan(0);
    }

    @Test
    @DisplayName("calcularIndicadores retorna cero cuando no hay ordenes")
    void calcularIndicadores_sinOrdenes() {
        when(ordenProduccionRepository.findByFechaFinBetween(any(), any())).thenReturn(List.of());

        IndicadoresProduccionResponseDTO resultado = service.calcularIndicadores(
                LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(resultado.getTotalOrdenesPeriodo()).isZero();
        assertThat(resultado.getPorcentajeCumplimiento()).isZero();
        assertThat(resultado.getCantidadTotalPlanificada()).isEqualByComparingTo("0.00");
        assertThat(resultado.getCantidadTotalProducida()).isEqualByComparingTo("0.00");
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
