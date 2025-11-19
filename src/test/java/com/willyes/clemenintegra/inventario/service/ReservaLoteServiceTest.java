package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.ReservaLote;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaLoteServiceTest {

    @Mock
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;

    @InjectMocks
    private ReservaLoteService service;

    @Test
    @DisplayName("crearOActualizarDesdeDetalle normaliza cantidades a escala 6")
    void crearOActualizarDesdeDetalle_normalizaEscala() {
        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .id(10L)
                .cantidad(new BigDecimal("3.45678901"))
                .lote(LoteProducto.builder().id(5L).build())
                .build();

        LoteProducto lote = LoteProducto.builder()
                .id(5L)
                .stockLote(new BigDecimal("10"))
                .almacen(new Almacen())
                .build();

        when(loteProductoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(lote));
        when(reservaLoteRepository.findByDetalleIdAndLoteIdForUpdate(10L, 5L)).thenReturn(Optional.empty());
        when(reservaLoteRepository.sumPendienteByLoteId(eq(5L), any())).thenReturn(BigDecimal.ZERO);
        when(reservaLoteRepository.save(any(ReservaLote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservaLote reserva = service.crearOActualizarDesdeDetalle(detalle);

        assertThat(reserva.getCantidadReservada().scale()).isEqualTo(6);
        assertThat(reserva.getCantidadReservada()).isEqualByComparingTo(new BigDecimal("3.456789"));
        assertThat(reserva.getCantidadConsumida()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("liberarReservasPorOrden cancela reservas activas y recalcula stock")
    void liberarReservasPorOrden_cancelaReservas() {
        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(20L);
        LoteProducto lote = LoteProducto.builder().id(8L).stockLote(new BigDecimal("15")).build();
        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(99L);
        solicitud.setDetalles(List.of(detalle));

        ReservaLote reserva = ReservaLote.builder()
                .id(30L)
                .lote(lote)
                .cantidadReservada(new BigDecimal("4.000000"))
                .cantidadConsumida(BigDecimal.ZERO)
                .estado(EstadoReservaLote.ACTIVA)
                .solicitudMovimientoDetalle(detalle)
                .build();

        when(solicitudMovimientoRepository.findWithDetalles(1L, null, null, null, false, List.of()))
                .thenReturn(List.of(solicitud));
        when(reservaLoteRepository.findBySolicitudMovimientoDetalleId(20L))
                .thenReturn(List.of(reserva));
        when(reservaLoteRepository.save(any(ReservaLote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(loteProductoRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(lote));
        when(reservaLoteRepository.sumPendienteByLoteId(eq(8L), any())).thenReturn(BigDecimal.ZERO);

        service.liberarReservasPorOrden(1L);

        ArgumentCaptor<ReservaLote> captor = ArgumentCaptor.forClass(ReservaLote.class);
        verify(reservaLoteRepository).save(captor.capture());
        ReservaLote actualizado = captor.getValue();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoReservaLote.CANCELADA);
        assertThat(actualizado.getCantidadConsumida()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        verify(loteProductoRepository).findByIdForUpdate(8L);
    }

    @Test
    @DisplayName("crearOActualizarDesdeDetalle insuficiente expone mensaje enriquecido")
    void crearOActualizarDesdeDetalle_insuficienteMensaje() {
        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .id(30L)
                .cantidad(new BigDecimal("6"))
                .lote(LoteProducto.builder().id(15L).build())
                .build();

        Producto producto = new Producto();
        producto.setCodigoSku("MP-COLRO");
        producto.setNombre("Colorante Rojo");
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setNombre("MILILITRO");
        producto.setUnidadMedida(unidadMedida);

        LoteProducto lote = LoteProducto.builder()
                .id(15L)
                .codigoLote("L-15")
                .producto(producto)
                .stockLote(new BigDecimal("5"))
                .stockReservado(new BigDecimal("2"))
                .almacen(new Almacen())
                .build();

        when(loteProductoRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(lote));
        when(reservaLoteRepository.findByDetalleIdAndLoteIdForUpdate(30L, 15L)).thenReturn(Optional.empty());
        when(reservaLoteRepository.sumPendienteByLoteId(eq(15L), any())).thenReturn(new BigDecimal("2"));

        assertThatThrownBy(() -> service.crearOActualizarDesdeDetalle(detalle))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("MP-COLRO")
                .hasMessageContaining("Colorante Rojo")
                .hasMessageContaining("MILILITRO")
                .hasMessageContaining("3.000000");
    }
}
