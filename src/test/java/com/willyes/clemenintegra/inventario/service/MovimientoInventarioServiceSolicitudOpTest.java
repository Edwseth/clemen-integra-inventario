package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.inventario.dto.AtencionDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceSolicitudOpTest {

    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private OrdenCompraService ordenCompraService;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private MovimientoInventarioMapper mapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private ReservaLoteService reservaLoteService;
    @Mock
    private RecepcionOCService recepcionOCService;
    @Mock
    private RetencionLoteService retencionLoteService;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @BeforeEach
    void setupSecurity() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("tester", "secret");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrarMovimiento_solicitudOp_consumeReservaYEsIdempotente() {
        Producto producto = new Producto();
        producto.setId(1);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(99L);
        producto.setUnidadMedida(unidad);

        LoteProducto lote = new LoteProducto();
        lote.setId(100L);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(10));
        lote.setStockLote(new BigDecimal("1000"));
        lote.setStockReservado(new BigDecimal("1000"));
        lote.setEstado(EstadoLote.DISPONIBLE);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(300L);
        detalle.setCantidad(new BigDecimal("1000"));
        detalle.setEstado(EstadoSolicitudMovimientoDetalle.PENDIENTE);
        detalle.setCantidadAtendida(BigDecimal.ZERO);
        detalle.setLote(lote);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(200L);
        solicitud.setProducto(producto);
        solicitud.setLote(lote);
        solicitud.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        solicitud.setEstado(EstadoSolicitudMovimiento.PENDIENTE);
        solicitud.setCantidad(new BigDecimal("1000"));
        solicitud.setDetalles(List.of(detalle));
        detalle.setSolicitudMovimiento(solicitud);

        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(400L);
        solicitud.setOrdenProduccion(ordenProduccion);

        Usuario usuario = Usuario.builder()
                .id(50L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .activo(true)
                .bloqueado(false)
                .build();
        solicitud.setUsuarioResponsable(usuario);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        AtencionDTO atencion = new AtencionDTO();
        atencion.setDetalleId(detalle.getId());
        atencion.setLoteId(lote.getId());
        atencion.setCantidad(new BigDecimal("1000"));
        atencion.setAlmacenOrigenId(10);
        atencion.setAlmacenDestinoId(20);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                "DOC-1",
                null,
                producto.getId(),
                lote.getId(),
                10,
                20,
                null,
                null,
                null,
                50L,
                solicitud.getId(),
                usuario.getId(),
                ordenProduccion.getId(),
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion)
        );

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(1L)).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(50L)).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(solicitudMovimientoRepository.findByIdWithLock(200L)).willReturn(Optional.of(solicitud));
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuario);
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(ordenProduccion.getId()))).willReturn(ordenProduccion);
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Object id = invocation.getArgument(1);
            return new Almacen(id instanceof Integer ? (Integer) id : ((Long) id).intValue());
        });
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        given(loteProductoRepository.save(any(LoteProducto.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(solicitudMovimientoDetalleRepository.findById(detalle.getId())).willReturn(Optional.of(detalle));
        given(solicitudMovimientoDetalleRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(solicitudMovimientoRepository.saveAndFlush(solicitud)).willReturn(solicitud);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(900L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(900L).build());
        given(catalogResolver.decimals(any())).willReturn(2);

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(900L);
        assertThat(detalle.getCantidadAtendida()).isEqualByComparingTo(new BigDecimal("1000.000000"));
        assertThat(detalle.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMovimiento.CERRADA);
        assertThat(solicitud.getFechaResolucion()).isNotNull();
        assertThat(lote.getStockLote()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(lote.getStockReservado()).isEqualByComparingTo(BigDecimal.ZERO);

        BigDecimal stockTrasPrimeraAprobacion = lote.getStockLote();
        BigDecimal reservaTrasPrimeraAprobacion = lote.getStockReservado();
        BigDecimal cantidadAtendidaTrasPrimeraAprobacion = detalle.getCantidadAtendida();

        MovimientoInventarioResponseDTO respuestaIdempotente = service.registrarMovimiento(dto);

        assertThat(respuestaIdempotente).isNotNull();
        assertThat(respuestaIdempotente.getId()).isNull();
        assertThat(respuestaIdempotente.getSolicitudId()).isEqualTo(solicitud.getId());
        assertThat(respuestaIdempotente.getEstadoSolicitud()).isEqualTo(EstadoSolicitudMovimiento.CERRADA);
        assertThat(respuestaIdempotente.getOrdenProduccionId()).isEqualTo(ordenProduccion.getId());
        assertThat(respuestaIdempotente.getDetallesSolicitud()).hasSize(1);
        MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO detalleRespuesta =
                respuestaIdempotente.getDetallesSolicitud().get(0);
        assertThat(detalleRespuesta.getDetalleId()).isEqualTo(detalle.getId());
        assertThat(detalleRespuesta.getCantidadAtendida()).isEqualByComparingTo(new BigDecimal("1000.000000"));
        assertThat(detalleRespuesta.getEstadoDetalle()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);

        assertThat(lote.getStockLote()).isEqualByComparingTo(stockTrasPrimeraAprobacion);
        assertThat(lote.getStockReservado()).isEqualByComparingTo(reservaTrasPrimeraAprobacion);
        assertThat(detalle.getCantidadAtendida()).isEqualByComparingTo(cantidadAtendidaTrasPrimeraAprobacion);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMovimiento.CERRADA);

        verify(movimientoInventarioRepository, times(1)).save(any(MovimientoInventario.class));
        verify(solicitudMovimientoRepository, times(1)).saveAndFlush(solicitud);
        verify(reservaLoteService, times(1)).consumirReserva(eq(solicitud), eq(detalle), eq(lote), eq(new BigDecimal("1000.000000")));
        verify(mapper, times(1)).safeToResponseDTO(any(MovimientoInventario.class));
    }

    @Test
    void transferenciaInterna_consumoTotalReservaPropia_noGeneraError() {
        Producto producto = new Producto();
        producto.setId(34);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(12L);
        producto.setUnidadMedida(unidad);

        LoteProducto loteOrigen = new LoteProducto();
        loteOrigen.setId(55L);
        loteOrigen.setProducto(producto);
        loteOrigen.setCodigoLote("LOT-55");
        loteOrigen.setAlmacen(new Almacen(10));
        loteOrigen.setStockLote(new BigDecimal("250"));
        loteOrigen.setStockReservado(new BigDecimal("250"));
        loteOrigen.setEstado(EstadoLote.DISPONIBLE);

        LoteProducto loteDestino = new LoteProducto();
        loteDestino.setId(75L);
        loteDestino.setProducto(producto);
        loteDestino.setCodigoLote("LOT-55");
        loteDestino.setAlmacen(new Almacen(20));
        loteDestino.setEstado(EstadoLote.DISPONIBLE);
        loteDestino.setStockLote(BigDecimal.ZERO);
        loteDestino.setStockReservado(BigDecimal.ZERO);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(701L);
        detalle.setCantidad(new BigDecimal("250"));
        detalle.setCantidadAtendida(BigDecimal.ZERO);
        detalle.setEstado(EstadoSolicitudMovimientoDetalle.PENDIENTE);
        detalle.setLote(loteOrigen);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(501L);
        solicitud.setEstado(EstadoSolicitudMovimiento.AUTORIZADA);
        solicitud.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        solicitud.setDetalles(List.of(detalle));
        detalle.setSolicitudMovimiento(solicitud);

        Usuario usuario = Usuario.builder()
                .id(80L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .activo(true)
                .bloqueado(false)
                .build();

        AtencionDTO atencion = new AtencionDTO();
        atencion.setDetalleId(detalle.getId());
        atencion.setLoteId(loteOrigen.getId());
        atencion.setCantidad(new BigDecimal("250"));
        atencion.setAlmacenOrigenId(10);
        atencion.setAlmacenDestinoId(20);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("250"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                "DOC-55",
                null,
                producto.getId(),
                loteOrigen.getId(),
                10,
                20,
                null,
                null,
                null,
                70L,
                solicitud.getId(),
                usuario.getId(),
                999L,
                null,
                loteOrigen.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion)
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        prepararEscenarioComun(dto, producto, solicitud, detalle, loteOrigen, loteDestino, movimientoEntidad, usuario);

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(900L);
        assertThat(loteOrigen.getStockLote()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(loteOrigen.getStockReservado()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
    }

    @Test
    void transferenciaInterna_bloqueadaCuandoReservaEsDeOtros() {
        Producto producto = new Producto();
        producto.setId(34);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(12L);
        producto.setUnidadMedida(unidad);

        LoteProducto loteOrigen = new LoteProducto();
        loteOrigen.setId(55L);
        loteOrigen.setProducto(producto);
        loteOrigen.setCodigoLote("LOT-55");
        loteOrigen.setAlmacen(new Almacen(10));
        loteOrigen.setStockLote(new BigDecimal("250"));
        loteOrigen.setStockReservado(new BigDecimal("250"));
        loteOrigen.setEstado(EstadoLote.DISPONIBLE);

        LoteProducto loteDestino = new LoteProducto();
        loteDestino.setId(80L);
        loteDestino.setProducto(producto);
        loteDestino.setCodigoLote("LOT-55");
        loteDestino.setAlmacen(new Almacen(20));
        loteDestino.setEstado(EstadoLote.DISPONIBLE);

        LoteProducto loteReservaAjena = new LoteProducto();
        loteReservaAjena.setId(500L);
        loteReservaAjena.setProducto(producto);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(702L);
        detalle.setCantidad(new BigDecimal("100"));
        detalle.setCantidadAtendida(BigDecimal.ZERO);
        detalle.setEstado(EstadoSolicitudMovimientoDetalle.PENDIENTE);
        detalle.setLote(loteReservaAjena);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(777L);
        solicitud.setEstado(EstadoSolicitudMovimiento.AUTORIZADA);
        solicitud.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        solicitud.setDetalles(List.of(detalle));
        detalle.setSolicitudMovimiento(solicitud);

        Usuario usuario = Usuario.builder()
                .id(81L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .activo(true)
                .bloqueado(false)
                .build();

        AtencionDTO atencion = new AtencionDTO();
        atencion.setDetalleId(detalle.getId());
        atencion.setLoteId(loteOrigen.getId());
        atencion.setCantidad(new BigDecimal("10"));
        atencion.setAlmacenOrigenId(10);
        atencion.setAlmacenDestinoId(20);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                "DOC-56",
                null,
                producto.getId(),
                loteOrigen.getId(),
                10,
                20,
                null,
                null,
                null,
                71L,
                solicitud.getId(),
                usuario.getId(),
                999L,
                null,
                loteOrigen.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion)
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        prepararEscenarioComun(dto, producto, solicitud, detalle, loteOrigen, loteDestino, movimientoEntidad, usuario);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("LOTE_STOCK_INSUFICIENTE");
    }

    private void prepararEscenarioComun(MovimientoInventarioDTO dto,
                                        Producto producto,
                                        SolicitudMovimiento solicitud,
                                        SolicitudMovimientoDetalle detalle,
                                        LoteProducto loteOrigen,
                                        LoteProducto loteDestino,
                                        MovimientoInventario movimientoEntidad,
                                        Usuario usuario) {
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(dto.tipoMovimientoDetalleId())).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(solicitudMovimientoRepository.findByIdWithLock(solicitud.getId())).willReturn(Optional.of(solicitud));
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuario);
        OrdenProduccion opReferencia = new OrdenProduccion();
        opReferencia.setId(dto.ordenProduccionId());
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(dto.ordenProduccionId()))).willReturn(opReferencia);
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Number id = invocation.getArgument(1);
            return new Almacen(id.intValue());
        });
        given(loteProductoRepository.findByIdForUpdate(loteOrigen.getId())).willReturn(Optional.of(loteOrigen));
        given(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                eq(loteOrigen.getCodigoLote()), eq(producto.getId()), eq(dto.almacenDestinoId()))).willReturn(Optional.of(loteDestino));
        given(loteProductoRepository.save(any(LoteProducto.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(solicitudMovimientoDetalleRepository.findById(detalle.getId())).willReturn(Optional.of(detalle));
        given(solicitudMovimientoDetalleRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(solicitudMovimientoDetalleRepository.countBySolicitudMovimientoIdAndEstadoNot(anyLong(), any())).willReturn(0L);
        given(solicitudMovimientoDetalleRepository.calcularReservaPendientePorSolicitudYLote(anyLong(), anyLong()))
                .willReturn(BigDecimal.ZERO);
        given(solicitudMovimientoRepository.saveAndFlush(solicitud)).willReturn(solicitud);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(900L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(900L).build());
        given(catalogResolver.decimals(any())).willReturn(2);
    }
}

