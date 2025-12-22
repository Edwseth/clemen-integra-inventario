package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ResumenAlertasCalidadDTO;
import com.willyes.clemenintegra.calidad.model.enums.TipoAlertaLoteCalidad;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertasCalidadServiceTest {

    @Mock
    private LoteProductoRepository loteProductoRepository;

    @InjectMocks
    private AlertasCalidadService service;

    @BeforeEach
    void setUpClock() {
        service.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("Incluye lotes próximos a vencer dentro del umbral con diasParaVencer positivo")
    void incluyeLotesProximosAVencer() {
        LoteProducto lote = crearLote(1L, "L-PRX", EstadoLote.DISPONIBLE, LocalDateTime.parse("2025-01-20T00:00:00"));

        when(loteProductoRepository.findAlertasProximasVencer(any(), any(), any()))
                .thenReturn(List.of(lote));
        when(loteProductoRepository.findAlertasVencidos(any(), any())).thenReturn(List.of());
        when(loteProductoRepository.findAlertasPendientesLiberar(any())).thenReturn(List.of());

        ResumenAlertasCalidadDTO resumen = service.obtenerAlertas(30);

        assertThat(resumen.getLotesProximosVencer())
                .singleElement()
                .satisfies(alerta -> {
                    assertThat(alerta.getTipoAlerta()).isEqualTo(TipoAlertaLoteCalidad.PROXIMO_VENCER);
                    assertThat(alerta.getDiasParaVencer()).isEqualTo(5);
                });
        assertThat(resumen.getTotalProximosVencer()).isEqualTo(1);
    }

    @Test
    @DisplayName("Incluye lotes vencidos con diasParaVencer negativo")
    void incluyeLotesVencidos() {
        LoteProducto lote = crearLote(2L, "L-VENC", EstadoLote.LIBERADO, LocalDateTime.parse("2025-01-10T00:00:00"));

        when(loteProductoRepository.findAlertasProximasVencer(any(), any(), any()))
                .thenReturn(List.of());
        when(loteProductoRepository.findAlertasVencidos(any(), any())).thenReturn(List.of(lote));
        when(loteProductoRepository.findAlertasPendientesLiberar(any())).thenReturn(List.of());

        ResumenAlertasCalidadDTO resumen = service.obtenerAlertas(30);

        assertThat(resumen.getLotesVencidos())
                .singleElement()
                .satisfies(alerta -> {
                    assertThat(alerta.getTipoAlerta()).isEqualTo(TipoAlertaLoteCalidad.VENCIDO);
                    assertThat(alerta.getDiasParaVencer()).isEqualTo(-5);
                });
        assertThat(resumen.getTotalVencidos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Incluye lotes en cuarentena o retenido como pendientes de liberar")
    void incluyeLotesPendientesLiberar() {
        LoteProducto lote = crearLote(3L, "L-PEND", EstadoLote.RETENIDO, null);

        when(loteProductoRepository.findAlertasProximasVencer(any(), any(), any()))
                .thenReturn(List.of());
        when(loteProductoRepository.findAlertasVencidos(any(), any())).thenReturn(List.of());
        when(loteProductoRepository.findAlertasPendientesLiberar(any())).thenReturn(List.of(lote));

        ResumenAlertasCalidadDTO resumen = service.obtenerAlertas(30);

        assertThat(resumen.getLotesPendientesLiberar())
                .singleElement()
                .satisfies(alerta -> {
                    assertThat(alerta.getTipoAlerta()).isEqualTo(TipoAlertaLoteCalidad.PENDIENTE_LIBERAR);
                    assertThat(alerta.getDiasParaVencer()).isNull();
                });
        assertThat(resumen.getTotalPendientesLiberar()).isEqualTo(1);
    }

    private LoteProducto crearLote(Long id, String codigo, EstadoLote estado, LocalDateTime fechaVencimiento) {
        Producto producto = Producto.builder()
                .id(10)
                .codigoSku("SKU-10")
                .nombre("Producto X")
                .build();
        Almacen almacen = Almacen.builder()
                .id(20)
                .nombre("Almacén Central")
                .build();
        return LoteProducto.builder()
                .id(id)
                .codigoLote(codigo)
                .estado(estado)
                .fechaVencimiento(fechaVencimiento)
                .producto(producto)
                .almacen(almacen)
                .stockLote(java.math.BigDecimal.ONE)
                .build();
    }
}
