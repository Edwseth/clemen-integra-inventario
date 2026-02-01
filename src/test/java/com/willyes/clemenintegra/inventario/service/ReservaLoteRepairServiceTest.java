package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ReservaLoteRepairRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ReservaLoteRepairResultDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.ReservaLote;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoDetalleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaLoteRepairServiceTest {

    @Mock
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ReservaLoteRepairService service;

    @Test
    @DisplayName("repararReservasLote reasigna reservas activas al lote del almacén origen")
    void repararReservasLote_reasignaReserva() {
        Producto producto = new Producto();
        producto.setId(99);

        LoteProducto loteDestino = LoteProducto.builder()
                .id(2255L)
                .codigoLote("00049")
                .producto(producto)
                .stockLote(BigDecimal.ZERO)
                .almacen(Almacen.builder().id(6).build())
                .build();

        LoteProducto loteOrigen = LoteProducto.builder()
                .id(2148L)
                .codigoLote("00049")
                .producto(producto)
                .stockLote(new BigDecimal("10"))
                .almacen(Almacen.builder().id(1).build())
                .build();

        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .id(77L)
                .lote(loteDestino)
                .almacenOrigen(Almacen.builder().id(1).build())
                .build();

        ReservaLote reserva = ReservaLote.builder()
                .id(500L)
                .lote(loteDestino)
                .cantidadReservada(new BigDecimal("2"))
                .cantidadConsumida(BigDecimal.ZERO)
                .estado(EstadoReservaLote.ACTIVA)
                .solicitudMovimientoDetalle(detalle)
                .build();

        when(reservaLoteRepository.findByEstadoAndSolicitudMovimientoDetalleIdIn(
                EstadoReservaLote.ACTIVA, List.of(77L)))
                .thenReturn(List.of(reserva));
        when(solicitudMovimientoDetalleRepository.findById(77L)).thenReturn(Optional.of(detalle));
        when(loteProductoRepository.findByIdForUpdate(2255L)).thenReturn(Optional.of(loteDestino));
        when(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(99, "00049", 1))
                .thenReturn(Optional.of(loteOrigen));
        when(reservaLoteRepository.save(any(ReservaLote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaLoteRepository.sumPendienteActivaByLoteId(eq(2255L), any())).thenReturn(BigDecimal.ZERO);
        when(reservaLoteRepository.sumPendienteActivaByLoteId(eq(2148L), any())).thenReturn(BigDecimal.ZERO);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        ReservaLoteRepairResultDTO result = service.repararReservasLote(
                new ReservaLoteRepairRequestDTO(List.of(77L), null, 50));

        assertThat(result.actualizadas()).isEqualTo(1);
        assertThat(result.reservasActualizadas()).containsExactly(500L);
        assertThat(reserva.getLote().getId()).isEqualTo(2148L);
        assertThat(reserva.getCantidadReservada()).isEqualByComparingTo(new BigDecimal("2.000000"));
    }
}
