package com.willyes.clemenintegra.inventario.regularizacion.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.service.impl.RegularizacionTrazabilidadServiceImpl;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegularizacionTrazabilidadServiceImplTest {
    @Mock OrdenProduccionRepository ordenProduccionRepository;
    @Mock MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock LoteProductoRepository loteProductoRepository;
    @Mock MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock MovimientoInventarioService movimientoInventarioService;
    @Mock com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository regularizacionRepository;
    @Mock com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadDetalleRepository detalleRepository;
    @InjectMocks RegularizacionTrazabilidadServiceImpl service;

    @Test
    void op155_diferenciaNegativa_devuelveEmpaquesLifoSinTocarMp() {
        Usuario u = Usuario.builder().id(1L).build();
        when(regularizacionRepository.findByIdempotencyKey("idem-155")).thenReturn(Optional.empty());
        when(regularizacionRepository.save(any(com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad.class))).thenAnswer(i -> {
            com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad r = i.getArgument(0, com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad.class);
            if (r.getId() == null) r.setId(10L);
            return r;
        });
        when(tipoMovimientoDetalleRepository.findByDescripcion(anyString())).thenReturn(Optional.of(TipoMovimientoDetalle.builder().id(1L).descripcion("x").build()));
        when(motivoMovimientoRepository.findByMotivo(any())).thenReturn(Optional.of(MotivoMovimiento.builder().id(1L).build()));
        when(movimientoInventarioService.registrarMovimiento(any(), anyString())).thenReturn(MovimientoInventarioResponseDTO.builder().id(99L).build());

        OrdenProduccion op = OrdenProduccion.builder().id(155L).cantidadProgramada(new BigDecimal("1000")).cantidadProducidaAcumulada(new BigDecimal("1000")).build();
        when(ordenProduccionRepository.findById(155L)).thenReturn(Optional.of(op));

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(155L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA))
                .thenReturn(List.of(consumo(21, 100L, "50", true), consumo(21, 101L, "40", true), consumo(500, 300L, "90", false)));

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(155L, new BigDecimal("977"), "ACTA", "regularizacion op 155", false), "idem-155", u);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService, atLeastOnce()).registrarMovimiento(captor.capture(), anyString());
        List<MovimientoInventarioDTO> enviados = captor.getAllValues();
        assertThat(enviados).allMatch(m -> Objects.equals(m.ordenProduccionId(), 155L));
        assertThat(enviados).allMatch(m -> m.productoId() == 21); // mp (producto 500) no tocado
        assertThat(respuesta.cantidadProgramada()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(respuesta.diferencia()).isEqualByComparingTo(new BigDecimal("-23"));
        assertThat(respuesta.movimientos()).isNotEmpty();
    }

    @Test
    void op230_diferenciaPositiva_consumoAdicionalEmpaques() {
        Usuario u = Usuario.builder().id(2L).build();
        when(regularizacionRepository.findByIdempotencyKey("idem-230")).thenReturn(Optional.empty());
        when(regularizacionRepository.save(any(com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad.class))).thenAnswer(i -> {
            com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad r = i.getArgument(0, com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad.class);
            if (r.getId() == null) r.setId(11L);
            return r;
        });
        when(tipoMovimientoDetalleRepository.findByDescripcion(anyString())).thenReturn(Optional.of(TipoMovimientoDetalle.builder().id(1L).descripcion("x").build()));
        when(motivoMovimientoRepository.findByMotivo(any())).thenReturn(Optional.of(MotivoMovimiento.builder().id(1L).build()));
        when(movimientoInventarioService.registrarMovimiento(any(), anyString())).thenReturn(MovimientoInventarioResponseDTO.builder().id(101L).build());

        OrdenProduccion op = OrdenProduccion.builder().id(230L).cantidadProgramada(new BigDecimal("71")).cantidadProducidaAcumulada(new BigDecimal("71")).build();
        when(ordenProduccionRepository.findById(230L)).thenReturn(Optional.of(op));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(230L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA))
                .thenReturn(List.of(consumo(23, 200L, "10", true)));

        LoteProducto fefo = new LoteProducto(); fefo.setId(200L); fefo.setStockLote(new BigDecimal("10")); fefo.setStockReservado(BigDecimal.ZERO);
        when(loteProductoRepository.findFefoByProductoAndAlmacen(23L, 6)).thenReturn(List.of(fefo));

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(230L, new BigDecimal("72"), "ACTA", "regularizacion op 230", false), "idem-230", u);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService, atLeastOnce()).registrarMovimiento(captor.capture(), anyString());
        assertThat(captor.getAllValues()).anyMatch(m -> m.tipoMovimiento() == TipoMovimiento.SALIDA && m.productoId() == 23 && m.cantidad().compareTo(BigDecimal.ONE) == 0);
        assertThat(respuesta.cantidadProgramada()).isEqualByComparingTo(new BigDecimal("71"));
        assertThat(respuesta.diferencia()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(respuesta.movimientos()).isNotEmpty();
    }

    private MovimientoInventario consumo(int productoId, long loteId, String cantidad, boolean empaque) {
        CategoriaProducto c = new CategoriaProducto(); c.setId(empaque ? 2L : 1L);
        Producto p = new Producto(); p.setId(productoId); p.setCategoriaProducto(c);
        LoteProducto l = new LoteProducto(); l.setId(loteId);
        return MovimientoInventario.builder().id(loteId).producto(p).lote(l).cantidad(new BigDecimal(cantidad)).fechaIngreso(LocalDateTime.now()).build();
    }
}
