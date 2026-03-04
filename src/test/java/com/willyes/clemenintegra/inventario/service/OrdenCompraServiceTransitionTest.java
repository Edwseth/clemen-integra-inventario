package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.HistorialEstadoOrdenRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTransitionTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private HistorialEstadoOrdenRepository historialEstadoOrdenRepository;

    @InjectMocks
    private OrdenCompraService ordenCompraService;

    @Test
    void invalidTransitionReturnsConflictCode() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(1)
                .estado(EstadoOrdenCompra.ENVIADA)
                .detalles(List.of(detalle(new BigDecimal("5"), BigDecimal.ZERO)))
                .build();

        when(ordenCompraRepository.findByIdWithDetalles(1L)).thenReturn(Optional.of(orden));

        CustomUserDetails principal = buildUser(1L, RolUsuario.ROL_COMPRADOR);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class, () ->
                ordenCompraService.cambiarEstado(1L, EstadoOrdenCompra.CERRADA, principal, "no procede"));

        assertEquals(ApiErrorCode.OC_TRANSICION_INVALIDA, ex.getCode());
        verify(historialEstadoOrdenRepository, never()).save(any());
    }

    @Test
    void forbiddenRoleReturnsRolInsuficiente() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(2)
                .estado(EstadoOrdenCompra.CREADA)
                .detalles(List.of(detalle(new BigDecimal("3"), BigDecimal.ZERO)))
                .build();
        when(ordenCompraRepository.findByIdWithDetalles(2L)).thenReturn(Optional.of(orden));

        CustomUserDetails principal = buildUser(99L, RolUsuario.ROL_JEFE_ALMACENES);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class, () ->
                ordenCompraService.cambiarEstado(2L, EstadoOrdenCompra.ENVIADA, principal, null));

        assertEquals(ApiErrorCode.ROL_INSUFICIENTE, ex.getCode());
    }

    @Test
    void validTransitionCreatesHistorial() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(3)
                .estado(EstadoOrdenCompra.ENVIADA)
                .detalles(List.of(detalle(new BigDecimal("2"), BigDecimal.ZERO)))
                .build();
        when(ordenCompraRepository.findByIdWithDetalles(3L)).thenReturn(Optional.of(orden));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(a -> a.getArgument(0));
        when(historialEstadoOrdenRepository.save(any(HistorialEstadoOrden.class)))
                .thenAnswer(a -> a.getArgument(0));

        CustomUserDetails principal = buildUser(5L, RolUsuario.ROL_COMPRADOR);

        HistorialEstadoOrden historial = ordenCompraService.cambiarEstado(
                3L, EstadoOrdenCompra.CANCELADA, principal, "cancelar");

        assertEquals(EstadoOrdenCompra.CANCELADA, historial.getEstado());
        assertEquals(EstadoOrdenCompra.CANCELADA, orden.getEstado());
        assertEquals(5L, historial.getCambiadoPor().getId());
        verify(historialEstadoOrdenRepository).save(any(HistorialEstadoOrden.class));
    }

    @Test
    void atrasadasBranchUsesRepository() {
        Pageable pageable = Pageable.unpaged();
        // Firma actual repo: findListadoFiltrado(Pageable, boolean, Set<EstadoOrdenCompra>, EstadoOrdenCompra, TipoOrdenCompra, String)
        when(ordenCompraRepository.findListadoFiltrado(
                any(Pageable.class),
                anyBoolean(),
                org.mockito.ArgumentMatchers.<Set<EstadoOrdenCompra>>any(),
                nullable(EstadoOrdenCompra.class),
                isNull(),
                nullable(String.class)))
                .thenReturn(new PageImpl<>(List.of()));
        Page<?> result = ordenCompraService.listar(pageable, true);
        assertNotNull(result);
        verify(ordenCompraRepository).findListadoFiltrado(
                eq(pageable),
                eq(true),
                org.mockito.ArgumentMatchers.<Set<EstadoOrdenCompra>>any(),
                isNull(),
                isNull(),
                isNull());
    }

    @Test
    void transicionesPermitidasParaCompradorEnCreada() {
        OrdenCompra orden = OrdenCompra.builder()
                .estado(EstadoOrdenCompra.CREADA)
                .detalles(List.of(detalle(new BigDecimal("1"), BigDecimal.ZERO)))
                .build();

        List<EstadoOrdenCompra> result = ordenCompraService.transicionesPermitidas(
                orden, List.of(RolUsuario.ROL_COMPRADOR.name()));

        assertEquals(List.of(EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.CANCELADA), result);
    }

    @Test
    void transicionesPermitidasIncluyenCerradaParaJefeDeAlmacenes() {
        OrdenCompra orden = OrdenCompra.builder()
                .estado(EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE)
                .detalles(List.of(detalle(new BigDecimal("1"), new BigDecimal("1"))))
                .build();

        List<EstadoOrdenCompra> result = ordenCompraService.transicionesPermitidas(
                orden, List.of(RolUsuario.ROL_JEFE_ALMACENES.name()));

        assertEquals(List.of(EstadoOrdenCompra.CERRADA), result);
    }

    @Test
    void transicionesPermitidasRetornaVacioParaCerrada() {
        OrdenCompra orden = OrdenCompra.builder()
                .estado(EstadoOrdenCompra.CERRADA)
                .detalles(List.of(detalle(new BigDecimal("1"), new BigDecimal("1"))))
                .build();

        List<EstadoOrdenCompra> result = ordenCompraService.transicionesPermitidas(
                orden, List.of(RolUsuario.ROL_SUPER_ADMIN.name()));

        assertTrue(result.isEmpty());
    }

    @Test
    void transicionesPermitidasConRolNoAutorizadoLanza403() {
        OrdenCompra orden = OrdenCompra.builder()
                .estado(EstadoOrdenCompra.CREADA)
                .detalles(List.of(detalle(new BigDecimal("1"), BigDecimal.ZERO)))
                .build();

        CustomBusinessException ex = assertThrows(CustomBusinessException.class, () ->
                ordenCompraService.transicionesPermitidas(orden, List.of("ROL_ALMACENISTA")));

        assertEquals(ApiErrorCode.ROL_INSUFICIENTE, ex.getCode());
    }

    @Test
    void calculaFechaCompromisoEntregaConUnDetalle() {
        LocalDate fecha = LocalDate.of(2025, 12, 10);
        List<OrdenCompraDetalleRequestDTO> detalles = List.of(detalleRequest(fecha));

        LocalDate result = ordenCompraService.calcularFechaCompromisoEntrega(detalles);

        assertEquals(fecha, result);
    }

    @Test
    void calculaFechaCompromisoEntregaConDosDetallesEligeLaMasTemprana() {
        LocalDate fecha1 = LocalDate.of(2025, 12, 10);
        LocalDate fecha2 = LocalDate.of(2025, 12, 15);
        List<OrdenCompraDetalleRequestDTO> detalles = List.of(detalleRequest(fecha1), detalleRequest(fecha2));

        LocalDate result = ordenCompraService.calcularFechaCompromisoEntrega(detalles);

        assertEquals(fecha1, result);
    }

    @Test
    void calculaFechaCompromisoEntregaConDetallesSinFechaRetornaNull() {
        List<OrdenCompraDetalleRequestDTO> detalles = List.of(detalleRequest(null), detalleRequest(null));

        LocalDate result = ordenCompraService.calcularFechaCompromisoEntrega(detalles);

        assertNull(result);
    }

    @Test
    void determinarTipoOrdenServiciosCuandoTodosSinControl() {
        Producto servicioA = new Producto();
        servicioA.setModoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK);
        Producto servicioB = new Producto();
        servicioB.setModoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK);

        TipoOrdenCompra tipo = ordenCompraService.determinarTipoOrdenPorDetalles(List.of(servicioA, servicioB));

        assertEquals(TipoOrdenCompra.SERVICIOS, tipo);
    }

    @Test
    void determinarTipoOrdenBienesCuandoTodosConControl() {
        Producto bienA = new Producto();
        bienA.setModoControlInventario(ModoControlInventario.CONTROL_STOCK);
        Producto bienB = new Producto();
        bienB.setModoControlInventario(ModoControlInventario.CONTROL_STOCK);

        TipoOrdenCompra tipo = ordenCompraService.determinarTipoOrdenPorDetalles(List.of(bienA, bienB));

        assertEquals(TipoOrdenCompra.BIENES, tipo);
    }

    @Test
    void determinarTipoOrdenMezcladoLanza422() {
        Producto bien = new Producto();
        bien.setModoControlInventario(ModoControlInventario.CONTROL_STOCK);
        Producto servicio = new Producto();
        servicio.setModoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> ordenCompraService.determinarTipoOrdenPorDetalles(List.of(bien, servicio)));

        assertEquals(ApiErrorCode.OC_TIPO_MEZCLADO_NO_PERMITIDO, ex.getCode());
    }

    @Test
    void ejecutarServicioActualizaCantidadesEstadoEHistorial() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(10)
                .tipo(TipoOrdenCompra.SERVICIOS)
                .estado(EstadoOrdenCompra.ENVIADA)
                .detalles(List.of(detalle(new BigDecimal("3"), BigDecimal.ZERO)))
                .build();

        when(ordenCompraRepository.findByIdWithDetalles(10L)).thenReturn(Optional.of(orden));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(a -> a.getArgument(0));
        when(historialEstadoOrdenRepository.save(any(HistorialEstadoOrden.class)))
                .thenAnswer(a -> a.getArgument(0));

        HistorialEstadoOrden historial = ordenCompraService.ejecutarServicio(10L,
                buildUser(7L, RolUsuario.ROL_COMPRADOR), "servicio ejecutado");

        assertEquals(EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE, orden.getEstado());
        assertEquals(new BigDecimal("3"), orden.getDetalles().get(0).getCantidadRecibida());
        assertEquals(EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE, historial.getEstado());
    }

    private OrdenCompraDetalle detalle(BigDecimal cantidad, BigDecimal recibida) {
        return OrdenCompraDetalle.builder()
                .cantidad(cantidad)
                .cantidadRecibida(recibida)
                .valorTotal(BigDecimal.ONE)
                .valorUnitario(BigDecimal.ONE)
                .iva(BigDecimal.ZERO)
                .build();
    }

    private OrdenCompraDetalleRequestDTO detalleRequest(LocalDate fechaNecesidad) {
        return new OrdenCompraDetalleRequestDTO(
                1L,
                new BigDecimal("1.000"),
                new BigDecimal("2.500"),
                new BigDecimal("0.00"),
                fechaNecesidad
        );
    }

    private CustomUserDetails buildUser(Long id, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombreCompleto("Tester");
        usuario.setNombreUsuario("tester");
        usuario.setClave("pwd");
        usuario.setActivo(true);
        usuario.setBloqueado(false);
        return new CustomUserDetails(usuario);
    }
}
