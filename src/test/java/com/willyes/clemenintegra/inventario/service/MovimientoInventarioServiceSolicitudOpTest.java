package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AtencionDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

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
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private RecepcionOCService recepcionOCService;
    @Mock
    private LoteCalidadValidator loteCalidadValidator;
    @Mock
    private EntityManager entityManager;
    @Mock
    private EtapaProduccionRepository etapaProduccionRepository;
    @Mock
    private UbicacionFisicaRepository ubicacionFisicaRepository;

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

        LoteProducto loteDestino = new LoteProducto();
        loteDestino.setId(301L);
        loteDestino.setProducto(producto);
        loteDestino.setCodigoLote(lote.getCodigoLote());
        loteDestino.setAlmacen(new Almacen(40));
        loteDestino.setEstado(EstadoLote.DISPONIBLE);
        loteDestino.setStockLote(BigDecimal.ZERO);
        loteDestino.setStockReservado(BigDecimal.ZERO);

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
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion),
                null
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
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);

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
    void registrarMovimiento_solicitudOp_sinSolicitudIdUsaDetalleFallback() {
        Producto producto = new Producto();
        producto.setId(2);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(77L);
        producto.setUnidadMedida(unidad);

        LoteProducto lote = new LoteProducto();
        lote.setId(300L);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(30));
        lote.setCodigoLote("LOTE-300");
        lote.setStockLote(new BigDecimal("500"));
        lote.setStockReservado(new BigDecimal("500"));
        lote.setEstado(EstadoLote.DISPONIBLE);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(900L);
        detalle.setCantidad(new BigDecimal("500"));
        detalle.setCantidadAtendida(BigDecimal.ZERO);
        detalle.setEstado(EstadoSolicitudMovimientoDetalle.PENDIENTE);
        detalle.setLote(lote);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(901L);
        solicitud.setProducto(producto);
        solicitud.setLote(lote);
        solicitud.setEstado(EstadoSolicitudMovimiento.AUTORIZADA);
        solicitud.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        solicitud.setCantidad(new BigDecimal("500"));
        solicitud.setDetalles(List.of(detalle));
        detalle.setSolicitudMovimiento(solicitud);

        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(333L);
        solicitud.setOrdenProduccion(ordenProduccion);

        Usuario usuario = Usuario.builder()
                .id(88L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .activo(true)
                .bloqueado(false)
                .build();
        solicitud.setUsuarioResponsable(usuario);

        AtencionDTO atencion = new AtencionDTO();
        atencion.setDetalleId(detalle.getId());
        atencion.setLoteId(lote.getId());
        atencion.setCantidad(new BigDecimal("500"));
        atencion.setAlmacenOrigenId(30);
        atencion.setAlmacenDestinoId(40);

        LoteProducto loteDestino = new LoteProducto();
        loteDestino.setId(301L);
        loteDestino.setProducto(producto);
        loteDestino.setCodigoLote(lote.getCodigoLote());
        loteDestino.setAlmacen(new Almacen(40));
        loteDestino.setEstado(EstadoLote.DISPONIBLE);
        loteDestino.setStockLote(BigDecimal.ZERO);
        loteDestino.setStockReservado(BigDecimal.ZERO);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("500"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                "DOC-FALLBACK",
                null,
                producto.getId(),
                lote.getId(),
                30,
                40,
                null,
                null,
                null,
                66L,
                null,
                usuario.getId(),
                ordenProduccion.getId(),
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion),
                null
        );

        prepararEscenarioComun(dto, producto, solicitud, detalle, lote, loteDestino, movimientoEntidad, usuario);

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(900L);
        assertThat(detalle.getCantidadAtendida()).isEqualByComparingTo(new BigDecimal("500.000000"));
        assertThat(detalle.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMovimiento.CERRADA);

        verify(solicitudMovimientoRepository, atLeastOnce()).findByIdWithLock(solicitud.getId());
        verify(reservaLoteService, times(1)).consumirReserva(eq(solicitud), eq(detalle), eq(lote), eq(new BigDecimal("500.000000")));
    }

    @Test
    void salidaProduccion_conSolicitudYReservaPropia_consumoPermitido() {
        Producto producto = new Producto();
        producto.setId(547);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(1L);
        producto.setUnidadMedida(unidad);

        LoteProducto lote = new LoteProducto();
        lote.setId(61L);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(1));
        lote.setCodigoLote("LOTE-61");
        lote.setStockLote(new BigDecimal("5000"));
        lote.setStockReservado(new BigDecimal("3600"));
        lote.setEstado(EstadoLote.DISPONIBLE);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(16L);
        detalle.setCantidad(new BigDecimal("3600"));
        detalle.setCantidadAtendida(BigDecimal.ZERO);
        detalle.setEstado(EstadoSolicitudMovimientoDetalle.PENDIENTE);
        detalle.setLote(lote);
        detalle.setAlmacenOrigen(new Almacen(1));
        detalle.setAlmacenDestino(new Almacen(6));

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(16L);
        solicitud.setProducto(producto);
        solicitud.setLote(lote);
        solicitud.setTipoMovimiento(TipoMovimiento.SALIDA);
        solicitud.setEstado(EstadoSolicitudMovimiento.RESERVADA);
        solicitud.setCantidad(new BigDecimal("3600"));
        solicitud.setDetalles(List.of(detalle));
        detalle.setSolicitudMovimiento(solicitud);

        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(2L);
        solicitud.setOrdenProduccion(ordenProduccion);

        Usuario usuario = Usuario.builder()
                .id(99L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .activo(true)
                .bloqueado(false)
                .build();
        solicitud.setUsuarioResponsable(usuario);

        EtapaProduccion etapaProduccion = new EtapaProduccion();
        etapaProduccion.setId(77L);
        etapaProduccion.setFechaInicio(LocalDateTime.now());
        etapaProduccion.setFechaFin(null);
        etapaProduccion.setEstado(EstadoEtapa.EN_PROCESO);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("3600"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-OP",
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                99L,
                solicitud.getId(),
                usuario.getId(),
                ordenProduccion.getId(),
                etapaProduccion.getId(),
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(),
                null
        );

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(anyLong())).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(solicitudMovimientoRepository.findByIdWithLock(solicitud.getId())).willReturn(Optional.of(solicitud));
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuario);
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(ordenProduccion.getId()))).willReturn(ordenProduccion);
        given(etapaProduccionRepository.findById(etapaProduccion.getId())).willReturn(Optional.of(etapaProduccion));
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
            mov.setId(901L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(901L).build());
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(901L);
        assertThat(detalle.getCantidadAtendida()).isEqualByComparingTo(new BigDecimal("3600.000000"));
        assertThat(detalle.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMovimiento.CERRADA);
        assertThat(lote.getStockReservado()).isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
        assertThat(lote.getStockLote()).isEqualByComparingTo(new BigDecimal("1400.00"));
        verify(movimientoInventarioRepository).save(argThat(mov -> mov.getOrdenProduccionEtapa() != null
                && mov.getOrdenProduccionEtapa().getId().equals(etapaProduccion.getId())));

        verify(reservaLoteService, times(1))
                .consumirReserva(eq(solicitud), eq(detalle), eq(lote), eq(new BigDecimal("3600.000000")));
    }

    @Test
    void salidaProduccion_sinOrdenEnDto_resuelveEtapaDesdeSolicitud() {
        Producto producto = new Producto();
        producto.setId(547);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(1L);
        producto.setUnidadMedida(unidad);

        LoteProducto lote = new LoteProducto();
        lote.setId(61L);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(1));
        lote.setCodigoLote("LOTE-61");
        lote.setStockLote(new BigDecimal("5000"));
        lote.setStockReservado(new BigDecimal("3600"));
        lote.setEstado(EstadoLote.DISPONIBLE);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(16L);
        detalle.setCantidad(new BigDecimal("3600"));
        detalle.setCantidadAtendida(BigDecimal.ZERO);
        detalle.setEstado(EstadoSolicitudMovimientoDetalle.PENDIENTE);
        detalle.setLote(lote);
        detalle.setAlmacenOrigen(new Almacen(1));
        detalle.setAlmacenDestino(new Almacen(6));

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(16L);
        solicitud.setProducto(producto);
        solicitud.setLote(lote);
        solicitud.setTipoMovimiento(TipoMovimiento.SALIDA);
        solicitud.setEstado(EstadoSolicitudMovimiento.RESERVADA);
        solicitud.setCantidad(new BigDecimal("3600"));
        solicitud.setDetalles(List.of(detalle));
        detalle.setSolicitudMovimiento(solicitud);

        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(2L);
        solicitud.setOrdenProduccion(ordenProduccion);

        Usuario usuario = Usuario.builder()
                .id(99L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .activo(true)
                .bloqueado(false)
                .build();
        solicitud.setUsuarioResponsable(usuario);

        EtapaProduccion etapaProduccion = new EtapaProduccion();
        etapaProduccion.setId(77L);
        etapaProduccion.setOrdenProduccion(ordenProduccion);
        etapaProduccion.setFechaInicio(LocalDateTime.now());
        etapaProduccion.setFechaFin(null);
        etapaProduccion.setEstado(EstadoEtapa.EN_PROCESO);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("3600"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-OP",
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                99L,
                solicitud.getId(),
                usuario.getId(),
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(),
                null
        );

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(anyLong())).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(solicitudMovimientoRepository.findByIdWithLock(solicitud.getId())).willReturn(Optional.of(solicitud));
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuario);
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Object id = invocation.getArgument(1);
            return new Almacen(id instanceof Integer ? (Integer) id : ((Long) id).intValue());
        });
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        given(loteProductoRepository.save(any(LoteProducto.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(solicitudMovimientoDetalleRepository.findById(detalle.getId())).willReturn(Optional.of(detalle));
        given(solicitudMovimientoDetalleRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(solicitudMovimientoRepository.saveAndFlush(solicitud)).willReturn(solicitud);
        lenient().when(reservaLoteRepository.sumPendienteActivaByLoteId(anyLong(), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);
        given(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(ordenProduccion.getId()))
                .willReturn(1L);
        given(etapaProduccionRepository.findTopByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNullOrderByFechaInicioDescIdDesc(ordenProduccion.getId()))
                .willReturn(Optional.of(etapaProduccion));
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(902L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(902L).build());
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());
        MovimientoInventario movPersistido = movimientoCaptor.getValue();
        assertThat(movPersistido.getOrdenProduccionEtapa()).isNotNull();
        assertThat(movPersistido.getOrdenProduccionEtapa().getId()).isEqualTo(etapaProduccion.getId());
    }

    @Test
    void duplicarMovimientoBase_copiaEtapaYOrdenProduccion() {
        OrdenProduccion ordenProduccion = OrdenProduccion.builder().id(10L).build();
        EtapaProduccion etapaProduccion = EtapaProduccion.builder().id(20L).ordenProduccion(ordenProduccion).build();

        MovimientoInventario base = new MovimientoInventario();
        base.setCantidad(new BigDecimal("5.00"));
        base.setTipoMovimiento(TipoMovimiento.SALIDA);
        base.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        base.setOrdenProduccion(ordenProduccion);
        base.setOrdenProduccionEtapa(etapaProduccion);
        base.setLote(new LoteProducto());

        LoteProducto nuevoLote = new LoteProducto();
        nuevoLote.setId(55L);

        MovimientoInventario duplicado = ReflectionTestUtils.invokeMethod(
                service,
                "duplicarMovimientoBase",
                base,
                nuevoLote,
                new BigDecimal("3.50")
        );

        assertThat(duplicado).isNotNull();
        assertThat(duplicado.getOrdenProduccion()).isSameAs(ordenProduccion);
        assertThat(duplicado.getOrdenProduccionEtapa()).isNotNull();
        assertThat(duplicado.getOrdenProduccionEtapa().getId()).isEqualTo(etapaProduccion.getId());
        assertThat(duplicado.getLote()).isSameAs(nuevoLote);
        assertThat(duplicado.getCantidad()).isEqualByComparingTo(new BigDecimal("3.50"));
    }

    @Disabled("Requiere entorno de integración para validar consumo doble de lote")
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
        loteOrigen.setStockLote(new BigDecimal("500"));
        loteOrigen.setStockReservado(new BigDecimal("500"));
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
        detalle.setCantidad(new BigDecimal("500"));
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
        atencion.setCantidad(new BigDecimal("500"));
        atencion.setAlmacenOrigenId(10);
        atencion.setAlmacenDestinoId(20);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("500"),
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
                null,
                loteOrigen.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion),
                null
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        prepararEscenarioComun(dto, producto, solicitud, detalle, loteOrigen, loteDestino, movimientoEntidad, usuario);

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(900L);
        assertThat(detalle.getCantidadAtendida()).isEqualByComparingTo(new BigDecimal("500.000000"));
        assertThat(detalle.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMovimiento.CERRADA);
    }

    @Disabled("Requiere entorno de integración para validar bloqueo con reservas ajenas")
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
        loteOrigen.setStockLote(new BigDecimal("500"));
        loteOrigen.setStockReservado(new BigDecimal("500"));
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
        detalle.setCantidad(new BigDecimal("500"));
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
        atencion.setCantidad(new BigDecimal("100"));
        atencion.setAlmacenOrigenId(10);
        atencion.setAlmacenDestinoId(20);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("100"),
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
                null,
                loteOrigen.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion),
                null
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        prepararEscenarioComun(dto, producto, solicitud, detalle, loteOrigen, loteDestino, movimientoEntidad, usuario);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("LOTE_NO_DISPONIBLE_TRANSFERIR");
    }

    @Test
    void transferenciaInterna_sinSolicitudNiDetalle_sigueBloqueandoPorStockGeneral() {
        Producto producto = new Producto();
        producto.setId(400);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(12L);
        producto.setUnidadMedida(unidad);

        LoteProducto loteOrigen = new LoteProducto();
        loteOrigen.setId(44L);
        loteOrigen.setProducto(producto);
        loteOrigen.setCodigoLote("LOT-44");
        loteOrigen.setAlmacen(new Almacen(10));
        loteOrigen.setStockLote(new BigDecimal("2500"));
        loteOrigen.setStockReservado(new BigDecimal("27500"));
        loteOrigen.setEstado(EstadoLote.DISPONIBLE);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());

        AtencionDTO atencion = new AtencionDTO();
        atencion.setDetalleId(null);
        atencion.setLoteId(loteOrigen.getId());
        atencion.setCantidad(new BigDecimal("30000"));
        atencion.setAlmacenOrigenId(10);
        atencion.setAlmacenDestinoId(20);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("30000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                "DOC-SIN-SOL",
                null,
                producto.getId(),
                loteOrigen.getId(),
                10,
                20,
                null,
                null,
                null,
                66L,
                null,
                99L,
                null,
                null,
                null,
                loteOrigen.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion),
                null
        );

        Usuario usuario = Usuario.builder()
                .id(99L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .activo(true)
                .bloqueado(false)
                .build();

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(dto.tipoMovimientoDetalleId())).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuario);
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Number id = invocation.getArgument(1);
            return new Almacen(id.intValue());
        });
        final int[] loteFetchCount = {0};
        LoteProducto plantillaLote = clonarLote(loteOrigen);
        given(loteProductoRepository.findByIdForUpdate(loteOrigen.getId())).willAnswer(invocation -> {
            if (loteFetchCount[0]++ == 0) {
                return Optional.of(loteOrigen);
            }
            return Optional.of(clonarLote(plantillaLote));
        });
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("LOTE_NO_DISPONIBLE_TRANSFERIR");

        verify(solicitudMovimientoRepository, never()).findByIdWithLock(anyLong());
    }

    @Test
    void registrarMovimiento_permiteEntradaProduccionEnCuarentena() {
        Producto producto = productoSemiElaborado();
        LoteProducto lote = loteEnEstado(producto, EstadoLote.EN_CUARENTENA, BigDecimal.ZERO, 50);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("25"),
                TipoMovimiento.ENTRADA,
                ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO,
                "DOC-PT",
                null,
                producto.getId(),
                lote.getId(),
                null,
                lote.getAlmacen().getId(),
                null,
                null,
                null,
                1L,
                null,
                null,
                10L,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                List.<AtencionDTO>of(),
                null
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(TipoMovimiento.ENTRADA);
        movimientoEntidad.setClasificacion(ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO);

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle detalle = new TipoMovimientoDetalle();
        detalle.setId(1L);
        detalle.setDescripcion("ENTRADA OP");
        given(tipoMovimientoDetalleRepository.findById(1L)).willReturn(Optional.of(detalle));
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuarioBasico());
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(10L);
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(10L))).willReturn(orden);
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Number id = invocation.getArgument(1);
            return new Almacen(id.intValue());
        });
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        given(loteProductoRepository.save(any(LoteProducto.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(movimientoInventarioRepository.save(any())).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(1L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(1L).build());
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(lote.getStockLote()).isEqualByComparingTo(new BigDecimal("25"));
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
    }

    @Test
    void registrarMovimiento_bloqueaSalidaDesdeCuarentena() {
        Producto producto = productoSemiElaborado();
        LoteProducto lote = loteEnEstado(producto, EstadoLote.EN_CUARENTENA, new BigDecimal("10"), 60);

        EtapaProduccion etapaActiva = EtapaProduccion.builder()
                .id(300L)
                .fechaInicio(LocalDateTime.now())
                .estado(EstadoEtapa.EN_PROCESO)
                .ordenProduccion(OrdenProduccion.builder().id(30L).build())
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("2"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-SAL",
                null,
                producto.getId(),
                lote.getId(),
                lote.getAlmacen().getId(),
                null,
                null,
                null,
                null,
                2L,
                null,
                null,
                30L,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                List.<AtencionDTO>of(),
                null
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimientoEntidad.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);

        TipoMovimientoDetalle detalle = new TipoMovimientoDetalle();
        detalle.setId(2L);
        detalle.setDescripcion("SALIDA OP");

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(2L)).willReturn(Optional.of(detalle));
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Number id = invocation.getArgument(1);
            return new Almacen(id.intValue());
        });
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(30L))).willAnswer(invocation -> {
            OrdenProduccion op = new OrdenProduccion();
            op.setId(30L);
            return op;
        });
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuarioBasico());
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        doThrow(new CustomBusinessException(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO, "BLOQUEO"))
                .when(loteCalidadValidator).validarLoteUtilizable(lote);
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
        lenient().when(reservaLoteRepository.sumPendienteActivaByLoteId(anyLong(), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);
        given(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(30L))
                .willReturn(1L);
        given(etapaProduccionRepository.findTopByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNullOrderByFechaInicioDescIdDesc(30L))
                .willReturn(Optional.of(etapaActiva));

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO);
    }

    @Test
    void registrarMovimiento_salidaProduccionAsociaEtapaActiva() {
        Producto producto = new Producto();
        producto.setId(1);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(5L);
        producto.setUnidadMedida(unidad);

        LoteProducto lote = new LoteProducto();
        lote.setId(200L);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(10));
        lote.setStockLote(new BigDecimal("50"));
        lote.setStockReservado(BigDecimal.ZERO);
        lote.setEstado(EstadoLote.DISPONIBLE);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("5"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-SAL",
                null,
                producto.getId(),
                lote.getId(),
                lote.getAlmacen().getId(),
                null,
                null,
                null,
                null,
                2L,
                null,
                null,
                20L,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                List.<AtencionDTO>of(),
                null
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimientoEntidad.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);

        EtapaProduccion etapaActiva = EtapaProduccion.builder()
                .id(55L)
                .fechaInicio(LocalDateTime.now())
                .estado(EstadoEtapa.EN_PROCESO)
                .ordenProduccion(OrdenProduccion.builder().id(20L).build())
                .build();

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle detalle = new TipoMovimientoDetalle();
        detalle.setId(2L);
        detalle.setDescripcion("SALIDA OP");
        given(tipoMovimientoDetalleRepository.findById(2L)).willReturn(Optional.of(detalle));
        given(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(20L))
                .willReturn(1L);
        given(etapaProduccionRepository.findTopByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNullOrderByFechaInicioDescIdDesc(20L))
                .willReturn(Optional.of(etapaActiva));
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(20L))).willAnswer(invocation -> {
            OrdenProduccion op = new OrdenProduccion();
            op.setId(20L);
            return op;
        });
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Number id = invocation.getArgument(1);
            return new Almacen(id.intValue());
        });
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        lenient().when(reservaLoteRepository.sumPendienteActivaByLoteId(anyLong(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(901L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            return MovimientoInventarioResponseDTO.builder()
                    .id(mov.getId())
                    .ordenProduccionEtapaId(
                            mov.getOrdenProduccionEtapa() != null ? mov.getOrdenProduccionEtapa().getId() : null)
                    .build();
        });
        doNothing().when(loteCalidadValidator).validarLoteUtilizable(any());

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta.getOrdenProduccionEtapaId()).isEqualTo(55L);
        verify(movimientoInventarioRepository).save(argThat(mov ->
                mov.getOrdenProduccionEtapa() != null && mov.getOrdenProduccionEtapa().getId().equals(55L)));
    }

    @Test
    void registrarMovimiento_salidaProduccionSinEtapaActivaLanzaError() {
        Producto producto = new Producto();
        producto.setId(1);
        producto.setUnidadMedida(new UnidadMedida());

        LoteProducto lote = new LoteProducto();
        lote.setId(201L);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(10));
        lote.setStockLote(new BigDecimal("10"));
        lote.setStockReservado(BigDecimal.ZERO);
        lote.setEstado(EstadoLote.DISPONIBLE);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-SAL",
                null,
                producto.getId(),
                lote.getId(),
                lote.getAlmacen().getId(),
                null,
                null,
                null,
                null,
                2L,
                null,
                null,
                21L,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                List.<AtencionDTO>of(),
                null
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimientoEntidad.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(2L)).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(21L))
                .willReturn(0L);
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Number id = invocation.getArgument(1);
            return new Almacen(id.intValue());
        });
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(21L))).willAnswer(invocation -> {
            OrdenProduccion op = new OrdenProduccion();
            op.setId(21L);
            return op;
        });
        lenient().when(reservaLoteRepository.sumPendienteActivaByLoteId(anyLong(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.OP_SIN_ETAPA_ACTIVA);

        verify(movimientoInventarioRepository, never()).save(any(MovimientoInventario.class));
    }

    @Test
    void registrarMovimiento_trasladoPrebodegaNoExigeEtapaActiva() {
        ReflectionTestUtils.setField(service, "preBodegaId", 6);
        ReflectionTestUtils.setField(service, "tipoDetalleTransferenciaId", 2);

        Producto producto = new Producto();
        producto.setId(1);
        producto.setUnidadMedida(new UnidadMedida());

        Almacen origen = new Almacen(5);
        origen.setNombre("Principal Empaque");

        Almacen destino = new Almacen(6);
        destino.setNombre("Pre-Bodega Producción");

        LoteProducto lote = new LoteProducto();
        lote.setId(301L);
        lote.setProducto(producto);
        lote.setAlmacen(origen);
        lote.setStockLote(new BigDecimal("10"));
        lote.setStockReservado(BigDecimal.ZERO);
        lote.setEstado(EstadoLote.DISPONIBLE);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("3"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-SAL",
                null,
                producto.getId(),
                lote.getId(),
                origen.getId(),
                destino.getId(),
                null,
                null,
                null,
                2L,
                null,
                null,
                24L,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                List.<AtencionDTO>of(),
                null
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimientoEntidad.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(2L);
        given(tipoMovimientoDetalleRepository.findById(2L)).willReturn(Optional.of(tipoDetalle));
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Number id = invocation.getArgument(1);
            if (Objects.equals(id.longValue(), destino.getId().longValue())) {
                return destino;
            }
            Almacen almacen = new Almacen(id.intValue());
            almacen.setNombre("Principal Empaque");
            return almacen;
        });
        OrdenProduccion opReferencia = new OrdenProduccion();
        opReferencia.setId(24L);
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(24L))).willReturn(opReferencia);
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        given(loteProductoRepository.save(any(LoteProducto.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuarioOperativo());
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
        lenient().doNothing().when(loteCalidadValidator).validarLoteUtilizable(any());
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(902L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(902L).build());

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());

        MovimientoInventario guardado = movimientoCaptor.getValue();
        assertThat(respuesta.getId()).isEqualTo(902L);
        assertThat(guardado.getTipoMovimiento()).isEqualTo(TipoMovimiento.TRANSFERENCIA);
        assertThat(guardado.getClasificacion()).isEqualTo(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION);
        assertThat(guardado.getOrdenProduccionEtapa()).isNull();
        verifyNoInteractions(etapaProduccionRepository);
    }

    @Test
    void registrarMovimiento_descartaEtapaCorruptaSinFechaInicio() {
        Producto producto = new Producto();
        producto.setId(1);
        producto.setUnidadMedida(new UnidadMedida());

        LoteProducto lote = new LoteProducto();
        lote.setId(202L);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(10));
        lote.setStockLote(new BigDecimal("10"));
        lote.setStockReservado(BigDecimal.ZERO);
        lote.setEstado(EstadoLote.DISPONIBLE);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-SAL",
                null,
                producto.getId(),
                lote.getId(),
                lote.getAlmacen().getId(),
                null,
                null,
                null,
                null,
                2L,
                null,
                null,
                22L,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                List.<AtencionDTO>of(),
                null
        );

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimientoEntidad.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);

        EtapaProduccion corrupta = EtapaProduccion.builder()
                .id(88L)
                .fechaInicio(null)
                .fechaFin(null)
                .ordenProduccion(OrdenProduccion.builder().id(22L).build())
                .build();

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(2L)).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(22L))
                .willReturn(0L);
        given(etapaProduccionRepository.findTopByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNullOrderByFechaInicioDescIdDesc(22L))
                .willReturn(Optional.empty());
        given(entityManager.getReference(eq(Almacen.class), any())).willAnswer(invocation -> {
            Number id = invocation.getArgument(1);
            return new Almacen(id.intValue());
        });
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(22L))).willAnswer(invocation -> {
            OrdenProduccion op = new OrdenProduccion();
            op.setId(22L);
            return op;
        });
        lenient().when(reservaLoteRepository.sumPendienteActivaByLoteId(anyLong(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.OP_SIN_ETAPA_ACTIVA);

        verify(movimientoInventarioRepository, never()).save(any(MovimientoInventario.class));
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
        final int[] loteFetchCount = {0};
        given(loteProductoRepository.findByIdForUpdate(loteOrigen.getId())).willAnswer(invocation -> {
            if (loteFetchCount[0]++ == 0) {
                return Optional.of(loteOrigen);
            }
            return Optional.of(clonarLote(loteOrigen));
        });
        lenient().when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                eq(loteOrigen.getCodigoLote()), eq(producto.getId()), eq(dto.almacenDestinoId()))).thenReturn(Optional.of(loteDestino));
        given(loteProductoRepository.save(any(LoteProducto.class))).willAnswer(invocation -> invocation.getArgument(0));
        lenient().when(solicitudMovimientoDetalleRepository.findById(detalle.getId())).thenReturn(Optional.of(detalle));
        lenient().when(solicitudMovimientoDetalleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(solicitudMovimientoDetalleRepository.countBySolicitudMovimientoIdAndEstadoNot(anyLong(), any()))
                .thenReturn(0L);
        lenient().when(solicitudMovimientoDetalleRepository.calcularReservaPendientePorSolicitudYLote(anyLong(), anyLong()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(solicitudMovimientoRepository.saveAndFlush(solicitud)).thenReturn(solicitud);
        lenient().when(movimientoInventarioRepository.save(any(MovimientoInventario.class))).thenAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(900L);
            return mov;
        });
        lenient().when(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .thenReturn(MovimientoInventarioResponseDTO.builder().id(900L).build());
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
    }

    @Test
    void registrarMovimiento_conIdempotencyKeyExistente_evitaDuplicarOperacion() {
        MovimientoInventarioDTO dto = mock(MovimientoInventarioDTO.class);
        given(movimientoInventarioRepository.findByIdempotencyKey("dup-key"))
                .willReturn(Optional.of(new MovimientoInventario()));

        assertThatThrownBy(() -> service.registrarMovimiento(dto, "dup-key"))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.MOVIMIENTO_DUPLICADO);

        verify(movimientoInventarioRepository, never()).save(any(MovimientoInventario.class));
    }

    private Producto productoSemiElaborado() {
        Producto producto = new Producto();
        producto.setId(200);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        producto.setCategoriaProducto(categoria);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(20L);
        producto.setUnidadMedida(unidad);
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
        return producto;
    }

    private LoteProducto loteEnEstado(Producto producto, EstadoLote estado, BigDecimal stock, int almacenId) {
        LoteProducto lote = new LoteProducto();
        lote.setId(500L + almacenId);
        lote.setProducto(producto);
        lote.setEstado(estado);
        lote.setStockLote(stock);
        lote.setAlmacen(new Almacen(almacenId));
        return lote;
    }

    private Usuario usuarioBasico() {
        return Usuario.builder()
                .id(99L)
                .rol(RolUsuario.ROL_SUPER_ADMIN)
                .nombreUsuario("tester")
                .clave("secret")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .activo(true)
                .bloqueado(false)
                .build();
    }

    private Usuario usuarioOperativo() {
        return Usuario.builder()
                .id(77L)
                .rol(RolUsuario.ROL_ALMACENISTA)
                .nombreUsuario("operativo")
                .clave("secret")
                .nombreCompleto("Operativo")
                .correo("operativo@example.com")
                .activo(true)
                .bloqueado(false)
                .build();
    }

    private LoteProducto clonarLote(LoteProducto origen) {
        if (origen == null) {
            return null;
        }
        LoteProducto copia = new LoteProducto();
        copia.setId(origen.getId());
        copia.setProducto(origen.getProducto());
        copia.setCodigoLote(origen.getCodigoLote());
        copia.setAlmacen(origen.getAlmacen());
        copia.setStockLote(origen.getStockLote());
        copia.setStockReservado(origen.getStockReservado());
        copia.setEstado(origen.getEstado());
        return copia;
    }
}
