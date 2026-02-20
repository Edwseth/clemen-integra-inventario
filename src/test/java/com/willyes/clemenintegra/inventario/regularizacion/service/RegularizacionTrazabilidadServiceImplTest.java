package com.willyes.clemenintegra.inventario.regularizacion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadDetalle;
import com.willyes.clemenintegra.inventario.regularizacion.service.impl.RegularizacionTrazabilidadServiceImpl;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock FormulaProductoRepository formulaProductoRepository;
    @Mock com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository regularizacionRepository;
    @Mock com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadDetalleRepository detalleRepository;
    @InjectMocks RegularizacionTrazabilidadServiceImpl service;

    @Test
    void op98_like_diferenciaNegativa_devuelveEmpaquesConLoteConsumido() {
        Usuario u = Usuario.builder().id(1L).build();
        when(regularizacionRepository.findByIdempotencyKey("idem-98")).thenReturn(Optional.empty());
        when(regularizacionRepository.save(any(com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad.class))).thenAnswer(i -> {
            com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad r = i.getArgument(0, com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad.class);
            if (r.getId() == null) r.setId(10L);
            return r;
        });
        when(tipoMovimientoDetalleRepository.findByDescripcion(anyString())).thenReturn(Optional.of(TipoMovimientoDetalle.builder().id(1L).descripcion("x").build()));
        when(motivoMovimientoRepository.findByMotivo(any())).thenReturn(Optional.of(MotivoMovimiento.builder().id(1L).build()));
        when(movimientoInventarioService.registrarMovimiento(any(), anyString())).thenReturn(MovimientoInventarioResponseDTO.builder().id(99L).build());

        CategoriaProducto catEmp = new CategoriaProducto(); catEmp.setId(2L);
        Producto pt = new Producto(); pt.setId(10); pt.setCategoriaProducto(new CategoriaProducto());
        OrdenProduccion op = OrdenProduccion.builder().id(98L).producto(pt).cantidadProgramada(new BigDecimal("30")).cantidadProducidaAcumulada(new BigDecimal("30")).build();
        when(ordenProduccionRepository.findById(98L)).thenReturn(Optional.of(op));

        Producto insumo = new Producto(); insumo.setId(21); insumo.setCategoriaProducto(catEmp);
        DetalleFormula det = DetalleFormula.builder().insumo(insumo).cantidadNecesaria(BigDecimal.ONE).build();
        FormulaProducto formula = FormulaProducto.builder().detalles(List.of(det)).build();
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA)).thenReturn(Optional.of(formula));

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(98L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA))
                .thenReturn(List.of(consumo(21, 100L, "30", true)));

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(98L, new BigDecimal("28"), "ACTA", "regularizacion op 98", false), "idem-98", u);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService, atLeastOnce()).registrarMovimiento(captor.capture(), anyString());
        List<MovimientoInventarioDTO> enviados = captor.getAllValues();
        assertThat(enviados).allMatch(m -> Objects.equals(m.ordenProduccionId(), 98L));
        assertThat(enviados).anyMatch(m -> m.productoId() == 21
                && m.tipoMovimiento() == TipoMovimiento.ENTRADA
                && m.clasificacion() == ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION
                && m.loteId().equals(100L)
                && m.cantidad().compareTo(new BigDecimal("2")) == 0);
        verify(detalleRepository, atLeastOnce()).save(any(RegularizacionTrazabilidadDetalle.class));
        assertThat(respuesta.cantidadProgramada()).isEqualByComparingTo(new BigDecimal("30"));
        assertThat(respuesta.diferencia()).isEqualByComparingTo(new BigDecimal("-2"));
        assertThat(respuesta.movimientos()).isNotEmpty();
    }

    @Test
    void sinConsumoDebeFallar422_yNoGuardarCabecera() {
        Usuario u = Usuario.builder().id(2L).build();
        when(regularizacionRepository.findByIdempotencyKey("idem-fail")).thenReturn(Optional.empty());
        when(regularizacionRepository.save(any(com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad.class))).thenAnswer(i -> {
            com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad r = i.getArgument(0, com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad.class);
            if (r.getId() == null) r.setId(11L);
            return r;
        });

        CategoriaProducto catEmp = new CategoriaProducto(); catEmp.setId(2L);
        Producto pt = new Producto(); pt.setId(10); pt.setCategoriaProducto(new CategoriaProducto());
        OrdenProduccion op = OrdenProduccion.builder().id(98L).producto(pt).cantidadProgramada(new BigDecimal("30")).cantidadProducidaAcumulada(new BigDecimal("30")).build();
        when(ordenProduccionRepository.findById(98L)).thenReturn(Optional.of(op));

        Producto insumo = new Producto(); insumo.setId(21); insumo.setCategoriaProducto(catEmp);
        DetalleFormula det = DetalleFormula.builder().insumo(insumo).cantidadNecesaria(BigDecimal.ONE).build();
        FormulaProducto formula = FormulaProducto.builder().detalles(List.of(det)).build();
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA)).thenReturn(Optional.of(formula));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(98L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(98L, new BigDecimal("28"), "ACTA", "regularizacion op 98", false), "idem-fail", u))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("No hay consumo/lote");

        verify(movimientoInventarioService, never()).registrarMovimiento(any(), anyString());
        verify(detalleRepository, never()).save(any());
    }

    @Test
    void idempotencyRepetidaDebeRetornarMovimientosExistentes() {
        Usuario u = Usuario.builder().id(3L).build();
        RegularizacionTrazabilidad existente = RegularizacionTrazabilidad.builder()
                .id(50L)
                .ordenProduccionId(98L)
                .cantidadProgramada(new BigDecimal("30"))
                .cantidadReal(new BigDecimal("28"))
                .diferencia(new BigDecimal("-2"))
                .idempotencyKey("idem-dup")
                .usuario(u)
                .fechaIngreso(LocalDateTime.now())
                .build();
        when(regularizacionRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existente));

        RegularizacionTrazabilidadDetalle d = RegularizacionTrazabilidadDetalle.builder()
                .regularizacion(existente)
                .productoId(21L)
                .loteId(100L)
                .cantidad(new BigDecimal("2"))
                .tipo(TipoMovimiento.ENTRADA.name())
                .movimiento(MovimientoInventario.builder().id(900L).build())
                .build();
        when(detalleRepository.findByRegularizacionIdOrderByIdAsc(50L)).thenReturn(List.of(d));

        MovimientoInventario mi = MovimientoInventario.builder().id(900L)
                .clasificacion(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION).build();
        when(movimientoInventarioRepository.findById(900L)).thenReturn(Optional.of(mi));

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(98L, new BigDecimal("28"), "ACTA", "regularizacion op 98", false), "idem-dup", u);

        assertThat(respuesta.movimientos()).hasSize(1);
        assertThat(respuesta.movimientos().get(0).clasificacion()).isEqualTo(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION.name());
    }

    private MovimientoInventario consumo(int productoId, long loteId, String cantidad, boolean empaque) {
        CategoriaProducto c = new CategoriaProducto(); c.setId(empaque ? 2L : 1L);
        Producto p = new Producto(); p.setId(productoId); p.setCategoriaProducto(c);
        LoteProducto l = new LoteProducto(); l.setId(loteId);
        return MovimientoInventario.builder().id(loteId).producto(p).lote(l).cantidad(new BigDecimal(cantidad)).fechaIngreso(LocalDateTime.now()).build();
    }
}
