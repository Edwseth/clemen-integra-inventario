package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AtencionDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
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
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private RecepcionOCService recepcionOCService;
    @Mock
    private LoteCalidadValidator loteCalidadValidator;
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
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion)
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
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of()
        );

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(anyLong())).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(solicitudMovimientoRepository.findByIdWithLock(solicitud.getId())).willReturn(Optional.of(solicitud));
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

        verify(reservaLoteService, times(1))
                .consumirReserva(eq(solicitud), eq(detalle), eq(lote), eq(new BigDecimal("3600.000000")));
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
                loteOrigen.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of(atencion)
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
                .hasMessageContaining("LOTE_STOCK_INSUFICIENTE");

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
                Boolean.FALSE,
                List.of()
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
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                List.of()
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
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(usuarioBasico());
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        doThrow(new CustomBusinessException(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO, "BLOQUEO"))
                .when(loteCalidadValidator).validarLoteUtilizable(lote);
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
        lenient().when(reservaLoteRepository.sumPendienteActivaByLoteId(anyLong(), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO);
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

