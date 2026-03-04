package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.OcServicioEjecucionResponse;
import com.willyes.clemenintegra.inventario.model.HistorialEstadoOrden;
import com.willyes.clemenintegra.inventario.model.OcServicioEjecucion;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.HistorialEstadoOrdenRepository;
import com.willyes.clemenintegra.inventario.repository.OcServicioEjecucionRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.RecepcionOCRepository;
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
import java.time.LocalDateTime;
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
    @Mock
    private OcServicioEjecucionRepository ocServicioEjecucionRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private RecepcionOCRepository recepcionOCRepository;

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

    @Test
    void ejecutarServicioParcialUnaLineaActualizaRecibidosEstadoYLog() {
        OrdenCompraDetalle d1 = detalleConId(101L, new BigDecimal("5"), BigDecimal.ZERO);
        OrdenCompraDetalle d2 = detalleConId(102L, new BigDecimal("4"), BigDecimal.ZERO);
        OrdenCompra orden = OrdenCompra.builder()
                .id(20)
                .tipo(TipoOrdenCompra.SERVICIOS)
                .estado(EstadoOrdenCompra.ENVIADA)
                .detalles(List.of(d1, d2))
                .build();
        d1.setOrdenCompra(orden);
        d2.setOrdenCompra(orden);

        when(ordenCompraRepository.findByIdWithDetalles(20L)).thenReturn(Optional.of(orden));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(a -> a.getArgument(0));
        when(ocServicioEjecucionRepository.save(any(OcServicioEjecucion.class))).thenAnswer(a -> a.getArgument(0));
        when(historialEstadoOrdenRepository.save(any(HistorialEstadoOrden.class))).thenAnswer(a -> a.getArgument(0));

        OcServicioEjecucion ejecucion = ordenCompraService.ejecutarServicioParcial(
                20L, 101L, new BigDecimal("2"), LocalDateTime.now(), "avance 1", buildUser(7L, RolUsuario.ROL_COMPRADOR));

        assertEquals(new BigDecimal("2"), d1.getCantidadRecibida());
        assertEquals(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA, orden.getEstado());
        assertEquals(new BigDecimal("2"), ejecucion.getCantidadEjecutada());
        verify(ocServicioEjecucionRepository).save(any(OcServicioEjecucion.class));
        verify(historialEstadoOrdenRepository).save(any(HistorialEstadoOrden.class));
        verify(movimientoInventarioRepository, never()).save(any());
        verify(recepcionOCRepository, never()).save(any());
    }

    @Test
    void ejecutarServicioParcialCompletaRestanteYPasaARecibidaCompletamente() {
        OrdenCompraDetalle detalle = detalleConId(201L, new BigDecimal("5"), new BigDecimal("2"));
        OrdenCompra orden = OrdenCompra.builder()
                .id(21)
                .tipo(TipoOrdenCompra.SERVICIOS)
                .estado(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA)
                .detalles(List.of(detalle))
                .build();
        detalle.setOrdenCompra(orden);

        when(ordenCompraRepository.findByIdWithDetalles(21L)).thenReturn(Optional.of(orden));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(a -> a.getArgument(0));
        when(ocServicioEjecucionRepository.save(any(OcServicioEjecucion.class))).thenAnswer(a -> a.getArgument(0));
        when(historialEstadoOrdenRepository.save(any(HistorialEstadoOrden.class))).thenAnswer(a -> a.getArgument(0));

        ordenCompraService.ejecutarServicioParcial(
                21L, 201L, new BigDecimal("3"), null, "completa", buildUser(9L, RolUsuario.ROL_COMPRADOR));

        assertEquals(new BigDecimal("5"), detalle.getCantidadRecibida());
        assertEquals(EstadoOrdenCompra.RECIBIDA_COMPLETAMENTE, orden.getEstado());
    }

    @Test
    void ejecutarServicioParcialExcedePendienteLanza422() {
        OrdenCompraDetalle detalle = detalleConId(301L, new BigDecimal("5"), new BigDecimal("4"));
        OrdenCompra orden = OrdenCompra.builder()
                .id(22)
                .tipo(TipoOrdenCompra.SERVICIOS)
                .estado(EstadoOrdenCompra.ENVIADA)
                .detalles(List.of(detalle))
                .build();

        when(ordenCompraRepository.findByIdWithDetalles(22L)).thenReturn(Optional.of(orden));

        CustomBusinessException ex = assertThrows(CustomBusinessException.class, () ->
                ordenCompraService.ejecutarServicioParcial(22L, 301L, new BigDecimal("2"), null, null, buildUser(9L, RolUsuario.ROL_COMPRADOR)));

        assertEquals(ApiErrorCode.OC_SERVICIO_EXCEDE_PENDIENTE, ex.getCode());
    }

    @Test
    void ejecutarServicioParcialEnOcBienesLanza422() {
        OrdenCompraDetalle detalle = detalleConId(401L, new BigDecimal("5"), BigDecimal.ZERO);
        OrdenCompra orden = OrdenCompra.builder()
                .id(23)
                .tipo(TipoOrdenCompra.BIENES)
                .estado(EstadoOrdenCompra.ENVIADA)
                .detalles(List.of(detalle))
                .build();

        when(ordenCompraRepository.findByIdWithDetalles(23L)).thenReturn(Optional.of(orden));

        CustomBusinessException ex = assertThrows(CustomBusinessException.class, () ->
                ordenCompraService.ejecutarServicioParcial(23L, 401L, new BigDecimal("1"), null, null, buildUser(9L, RolUsuario.ROL_COMPRADOR)));

        assertEquals(ApiErrorCode.OC_NO_ES_SERVICIO, ex.getCode());
    }

    @Test
    void listarEjecucionesServicioRetornaOrdenadoPorFechaDesc() {
        OrdenCompra orden = OrdenCompra.builder().id(24).tipo(TipoOrdenCompra.SERVICIOS).build();
        OrdenCompraDetalle detalle = detalleConId(501L, new BigDecimal("5"), BigDecimal.ZERO);
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto("Usuario Test");

        OcServicioEjecucion ejecucion = OcServicioEjecucion.builder()
                .ordenCompra(orden)
                .ordenCompraDetalle(detalle)
                .cantidadEjecutada(new BigDecimal("1"))
                .fechaEjecucion(LocalDateTime.now())
                .observaciones("obs")
                .usuario(usuario)
                .build();

        when(ordenCompraRepository.findById(24L)).thenReturn(Optional.of(orden));
        when(ocServicioEjecucionRepository.findByOrdenCompra_IdOrderByFechaEjecucionDesc(24L)).thenReturn(List.of(ejecucion));

        List<OcServicioEjecucionResponse> respuesta = ordenCompraService.listarEjecucionesServicio(24L);

        assertEquals(1, respuesta.size());
        assertEquals(501L, respuesta.get(0).detalleId());
        assertEquals("Usuario Test", respuesta.get(0).usuarioNombre());
    }

    private OrdenCompraDetalle detalleConId(Long id, BigDecimal cantidad, BigDecimal recibida) {
        OrdenCompraDetalle detalle = detalle(cantidad, recibida);
        detalle.setId(id);
        return detalle;
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
