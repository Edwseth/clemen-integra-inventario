package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.UbicacionFisica;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimientoInventarioServiceTransferenciaTest {

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
    private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;
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
    private UbicacionFisicaRepository ubicacionFisicaRepository;
    @Mock
    private EtapaProduccionRepository etapaProduccionRepository;

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @BeforeEach
    void setupSecurity() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("tester", "secret");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        lenient().when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(null);
    }

    @Test
    void transferenciaConLoteLiberadoEnOrigenSeAcepta() {
        Producto producto = crearProducto(8, 2);
        LoteProducto lote = crearLote(30L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("4000"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                99L,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                null
        );

        configurarMocksBasicos(producto, lote);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(200L);
            return mov;
        });
        ArgumentCaptor<LoteProducto> lotesGuardados = ArgumentCaptor.forClass(LoteProducto.class);
        given(loteProductoRepository.save(lotesGuardados.capture())).willAnswer(invocation -> {
            LoteProducto loteArg = invocation.getArgument(0);
            if (loteArg.getId() == null) {
                loteArg.setId(900L);
            }
            return loteArg;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(200L).build());

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(200L);
        assertThat(lote.getStockLote()).isEqualByComparingTo(new BigDecimal("3000.00"));
        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().getOrdenProduccionEtapa()).isNull();
        LoteProducto loteDestinoPersistido = lotesGuardados.getAllValues().stream()
                .filter(lp -> lp.getAlmacen() != null && lp.getAlmacen().getId().equals(dto.almacenDestinoId()))
                .reduce((first, second) -> second)
                .orElse(null);
        assertThat(loteDestinoPersistido).isNotNull();
        assertThat(loteDestinoPersistido.getStockLote()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(loteDestinoPersistido.getLoteOrigen()).isEqualTo(lote);
        assertThat(movimientoCaptor.getValue().getLote()).isSameAs(loteDestinoPersistido);
    }

    @Test
    void transferenciaConUbicacionValidaAsignaUbicacionDestino() {
        Producto producto = crearProducto(9, 2);
        LoteProducto lote = crearLote(40L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("2000"), BigDecimal.ZERO, false);
        Almacen almacenDestino = new Almacen(6);
        UbicacionFisica ubicacion = UbicacionFisica.builder()
                .id(99L)
                .almacen(almacenDestino)
                .codigo("A1")
                .activo(true)
                .build();
        LoteProducto loteDestino = crearLote(401L, producto, almacenDestino.getId(), EstadoLote.DISPONIBLE,
                BigDecimal.ZERO, BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("500"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                almacenDestino.getId(),
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                ubicacion.getId()
        );

        configurarMocksBasicos(producto, lote);
        given(ubicacionFisicaRepository.findByIdAndActivoTrue(ubicacion.getId()))
                .willReturn(Optional.of(ubicacion));
        given(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                lote.getCodigoLote(), producto.getId(), almacenDestino.getId()))
                .willReturn(Optional.of(loteDestino));

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(201L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(201L).build());

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta.getId()).isEqualTo(201L);
        assertThat(loteDestino.getUbicacionFisica()).isEqualTo(ubicacion);
    }

    @Test
    void movimientoOp_fuerzaDocReferenciaConCodigoOrden() {
        Producto producto = crearProducto(8, 2);
        LoteProducto lote = crearLote(30L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("4000"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                null,
                "Se realiza movimiento bajo la orden OP-CLEMEN-20260204-57",
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                100L,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                null
        );

        configurarMocksBasicos(producto, lote);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        movimientoEntidad.setDocReferencia(dto.docReferencia());

        OrdenProduccion op = new OrdenProduccion();
        op.setId(100L);
        op.setCodigoOrden("OP-CLEMEN-20260204-57");

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(entityManager.getReference(eq(OrdenProduccion.class), eq(100L))).willReturn(op);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(200L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(200L).build());

        service.registrarMovimiento(dto);

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository, times(1)).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().getDocReferencia()).isEqualTo("OP-CLEMEN-20260204-57");
    }

    @Test
    void movimientoNoOp_conDocReferenciaMayorA45_retorna422() {
        Producto producto = crearProducto(8, 2);
        LoteProducto lote = crearLote(30L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("4000"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                "DOC-REFERENCIA-DEMASIADO-LARGA-PARA-VALIDAR-LIMITE-45-CHARS",
                null,
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                null
        );

        configurarMocksBasicos(producto, lote);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        movimientoEntidad.setDocReferencia(dto.docReferencia());

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422 UNPROCESSABLE_ENTITY \"DOC_REFERENCIA_LARGA\"");

        verify(movimientoInventarioRepository, never()).save(any(MovimientoInventario.class));
    }

    @Test
    void transferenciaConLoteEnCuarentenaDevuelveErrorDeCalidad() {
        Producto producto = crearProducto(18, 2);
        LoteProducto lote = crearLote(301L, producto, 1, EstadoLote.EN_CUARENTENA,
                new BigDecimal("4000"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("500"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                null
        );

        configurarMocksBasicos(producto, lote);
        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        doThrow(new CustomBusinessException(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO,
                "Lote no liberado"))
                .when(loteCalidadValidator).validarLoteUtilizable(lote);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO));
    }

    @Test
    void transferenciaConStockInsuficienteDisparaError() {
        Producto producto = crearProducto(9, 2);
        LoteProducto lote = crearLote(31L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("500"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                null
        );

        configurarMocksBasicos(producto, lote);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("LOTE_NO_DISPONIBLE_TRANSFERIR");
    }

    @Test
    void transferenciaConUbicacionDeOtroAlmacenDevuelve422() {
        Producto producto = crearProducto(10, 2);
        LoteProducto lote = crearLote(32L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("500"), BigDecimal.ZERO, false);
        UbicacionFisica ubicacion = UbicacionFisica.builder()
                .id(123L)
                .almacen(new Almacen(99))
                .codigo("X1")
                .activo(true)
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("100"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                ubicacion.getId()
        );

        configurarMocksBasicos(producto, lote);
        given(ubicacionFisicaRepository.findByIdAndActivoTrue(ubicacion.getId()))
                .willReturn(Optional.of(ubicacion));

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ApiErrorCode.UBICACION_NO_PERTENECE_ALMACEN);
                });
    }

    @Test
    void registrarMovimiento_registraBitacoraParaMotivoCritico() {
        Producto producto = crearProducto(21, 2);
        LoteProducto lote = crearLote(50L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("4000"), BigDecimal.ZERO, false);
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                "DOC-123",
                null,
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                1L,
                5L,
                null,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                Boolean.FALSE,
                null
        );

        configurarMocksBasicos(producto, lote);
        given(usuarioService.obtenerUsuarioAutenticado())
                .willReturn(Usuario.builder().id(1L).nombreCompleto("Tester").build());
        given(movimientoInventarioRepository.findByIdempotencyKey("idem-1"))
                .willReturn(Optional.empty());
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(1L);
        motivo.setMotivo(ClasificacionMovimientoInventario.AJUSTE_NEGATIVO);
        given(motivoMovimientoRepository.findById(1L)).willReturn(Optional.of(motivo));

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        movimientoEntidad.setDocReferencia(dto.docReferencia());

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(300L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(300L).build());

        service.registrarMovimiento(dto, "idem-1");

        ArgumentCaptor<BitacoraCambiosInventarioDTO> bitacoraCaptor =
                ArgumentCaptor.forClass(BitacoraCambiosInventarioDTO.class);
        verify(bitacoraCambiosInventarioService).crear(bitacoraCaptor.capture());
        assertThat(bitacoraCaptor.getValue().getValorNuevo()).isNotNull();
        assertThat(bitacoraCaptor.getValue().getValorNuevo().length()).isLessThanOrEqualTo(255);
    }

    @Test
    void registrarMovimiento_noRegistraBitacoraParaMotivoNoCritico() {
        Producto producto = crearProducto(22, 2);
        LoteProducto lote = crearLote(51L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("4000"), BigDecimal.ZERO, false);
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                "DOC-456",
                null,
                null,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                11L,
                5L,
                null,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null,
                Boolean.FALSE,
                null
        );

        configurarMocksBasicos(producto, lote);
        given(usuarioService.obtenerUsuarioAutenticado())
                .willReturn(Usuario.builder().id(2L).nombreCompleto("Tester").build());
        given(movimientoInventarioRepository.findByIdempotencyKey("idem-2"))
                .willReturn(Optional.empty());
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(11L);
        motivo.setMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        given(motivoMovimientoRepository.findById(11L)).willReturn(Optional.of(motivo));

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        movimientoEntidad.setDocReferencia(dto.docReferencia());

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(301L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(301L).build());

        service.registrarMovimiento(dto, "idem-2");

        verify(bitacoraCambiosInventarioService, never()).crear(any());
    }


    @Test
    void salidaConCantidadDecimalMayorAlDisponible_retorna422ConDetalle() {
        Producto producto = crearProducto(517, 2);
        LoteProducto lote = crearLote(2845L, producto, 6, EstadoLote.LIBERADO,
                new BigDecimal("0.460000"), BigDecimal.ZERO.setScale(6), false);

        MovimientoInventarioDTO dto = buildTransferenciaDTO(
                new BigDecimal("0.468000"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC-DEC",
                producto.getId(),
                lote.getId(),
                6,
                null,
                11L,
                5L,
                359L,
                lote.getCodigoLote()
        );

        configurarMocksBasicos(producto, lote);
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(Usuario.builder().id(2L).build());
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(11L);
        motivo.setMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        given(motivoMovimientoRepository.findById(11L)).willReturn(Optional.of(motivo));
        given(mapper.toEntity(dto)).willReturn(new MovimientoInventario());

        assertThatThrownBy(() -> service.registrarMovimiento(dto, null))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(ex -> {
                    CustomBusinessException businessException = (CustomBusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(ApiErrorCode.LOTE_STOCK_INSUFICIENTE);
                    Map<String, Object> details = (Map<String, Object>) businessException.getDetails();
                    assertThat(details.get("productoId")).isEqualTo(517);
                    assertThat(details.get("loteId")).isEqualTo(2845L);
                    assertThat(details.get("almacenOrigenId")).isEqualTo(6);
                    assertThat((BigDecimal) details.get("solicitado")).isEqualByComparingTo(new BigDecimal("0.468000"));
                    assertThat((BigDecimal) details.get("disponible")).isEqualByComparingTo(new BigDecimal("0.460000"));
                    assertThat((BigDecimal) details.get("diferencia")).isEqualByComparingTo(new BigDecimal("0.008000"));
                });
    }

    @Test
    void transferenciaDecimalMantieneStockExactoSeisDecimales() {
        Producto producto = crearProducto(517, 2);
        LoteProducto loteOrigen = crearLote(2845L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("0.470000"), BigDecimal.ZERO.setScale(6), false);

        MovimientoInventarioDTO dto = buildTransferenciaDTO(
                new BigDecimal("0.470000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                "DOC-TR-DEC",
                producto.getId(),
                loteOrigen.getId(),
                1,
                6,
                5L,
                5L,
                null,
                loteOrigen.getCodigoLote()
        );

        configurarMocksBasicos(producto, loteOrigen);
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(Usuario.builder().id(2L).build());
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(5L);
        motivo.setMotivo(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL);
        given(motivoMovimientoRepository.findById(5L)).willReturn(Optional.of(motivo));

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        movimientoEntidad.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL);
        movimientoEntidad.setCantidad(new BigDecimal("0.470000"));
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class))).willReturn(MovimientoInventarioResponseDTO.builder().id(999L).build());

        service.registrarMovimiento(dto, null);

        assertThat(loteOrigen.getStockLote()).isEqualByComparingTo(new BigDecimal("0.000000"));
        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(captor.capture());
        assertThat(captor.getValue().getCantidad()).isEqualByComparingTo(new BigDecimal("0.470000"));
    }

    private void configurarMocksBasicos(Producto producto, LoteProducto lote) {
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(5L)).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        lenient().when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                lote.getCodigoLote(), producto.getId(), 6)).thenReturn(Optional.empty());
        lenient().when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto loteArgument = invocation.getArgument(0);
            if (loteArgument.getId() == null) {
                loteArgument.setId(900L);
            }
            return loteArgument;
        });
        lenient().when(entityManager.getReference(eq(Almacen.class), any()))
                .thenAnswer(invocation -> {
                    Object id = invocation.getArgument(1);
                    return new Almacen(id instanceof Integer ? (Integer) id : ((Long) id).intValue());
                });
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
    }

    private MovimientoInventarioDTO buildTransferenciaDTO(BigDecimal cantidad,
                                                          TipoMovimiento tipoMovimiento,
                                                          ClasificacionMovimientoInventario clasificacion,
                                                          String docReferencia,
                                                          Integer productoId,
                                                          Long loteProductoId,
                                                          Integer almacenOrigenId,
                                                          Integer almacenDestinoId,
                                                          Long motivoMovimientoId,
                                                          Long tipoMovimientoDetalleId,
                                                          Long ordenProduccionId,
                                                          String codigoLote) {
        return new MovimientoInventarioDTO(
                null,
                cantidad,
                tipoMovimiento,
                clasificacion,
                docReferencia,
                null,
                null,
                null,
                null,
                productoId,
                loteProductoId,
                almacenOrigenId,
                almacenDestinoId,
                null,
                null,
                motivoMovimientoId,
                tipoMovimientoDetalleId,
                null,
                null,
                ordenProduccionId,
                null,
                null,
                codigoLote,
                null,
                null,
                Boolean.FALSE,
                List.of(),
                Boolean.FALSE,
                null
        );
    }

    private Producto crearProducto(Integer id, int escala) {
        Producto producto = new Producto();
        producto.setId(id);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(100L);
        producto.setUnidadMedida(unidad);
        lenient().when(catalogResolver.decimals(unidad)).thenReturn(escala);
        return producto;
    }

    private LoteProducto crearLote(Long id,
                                   Producto producto,
                                   Integer almacenId,
                                   EstadoLote estado,
                                   BigDecimal stock,
                                   BigDecimal reservado,
                                   boolean agotado) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setCodigoLote("L-" + id);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setEstado(estado);
        lote.setStockLote(stock);
        lote.setStockReservado(reservado);
        lote.setAgotado(agotado);
        return lote;
    }
}
