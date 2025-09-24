package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AtencionDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceImplTest {

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
    private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private InventoryCatalogResolver inventoryCatalogResolver;
    @Mock
    private ReservaLoteService reservaLoteService;
    @Mock
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @Test
    @SuppressWarnings("unchecked")
    void permiteTransferenciaUsandoReservaPendienteCuandoSolicitudAutorizada() {
        Almacen origen = new Almacen();
        origen.setId(1);
        origen.setNombre("Origen");
        origen.setUbicacion("Ubicacion");
        origen.setCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        origen.setTipo(TipoAlmacen.PRINCIPAL);

        Almacen destino = new Almacen();
        destino.setId(2);
        destino.setNombre("Destino");
        destino.setUbicacion("Ubicacion");
        destino.setCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        destino.setTipo(TipoAlmacen.PRINCIPAL);

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(10L);
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto producto = new Producto();
        producto.setId(100);
        producto.setCategoriaProducto(categoria);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO);

        LoteProducto loteOrigen = new LoteProducto();
        loteOrigen.setId(50L);
        loteOrigen.setCodigoLote("LOT-001");
        loteOrigen.setAlmacen(origen);
        loteOrigen.setProducto(producto);
        loteOrigen.setEstado(EstadoLote.DISPONIBLE);
        loteOrigen.setStockLote(new BigDecimal("10.00"));
        loteOrigen.setStockReservado(new BigDecimal("10.000000"));

        LoteProducto loteDestino = new LoteProducto();
        loteDestino.setId(51L);
        loteDestino.setCodigoLote("LOT-001");
        loteDestino.setAlmacen(destino);
        loteDestino.setProducto(producto);
        loteDestino.setEstado(EstadoLote.DISPONIBLE);
        loteDestino.setStockLote(new BigDecimal("0.00"));
        loteDestino.setStockReservado(new BigDecimal("0.000000"));

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(200L)
                .estado(EstadoSolicitudMovimiento.AUTORIZADA)
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .producto(producto)
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(new java.util.ArrayList<>())
                .build();

        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .id(300L)
                .solicitudMovimiento(solicitud)
                .lote(loteOrigen)
                .cantidad(new BigDecimal("10.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .build();
        solicitud.getDetalles().add(detalle);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10.000000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                producto.getId(),
                loteOrigen.getId(),
                origen.getId(),
                destino.getId(),
                null,
                null,
                null,
                99L,
                solicitud.getId(),
                null,
                null,
                null,
                loteOrigen.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of()
        );

        when(loteProductoRepository.findByIdForUpdate(loteOrigen.getId())).thenReturn(Optional.of(loteOrigen));
        when(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(
                producto.getId(), loteOrigen.getCodigoLote(), destino.getId()))
                .thenReturn(Optional.of(loteDestino));
        when(loteProductoRepository.save(any(LoteProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<MovimientoInventarioServiceImpl.MovimientoLoteDetalle> resultado =
                (List<MovimientoInventarioServiceImpl.MovimientoLoteDetalle>) ReflectionTestUtils.invokeMethod(
                        service,
                        "procesarMovimientoConLoteExistente",
                        dto,
                        TipoMovimiento.TRANSFERENCIA,
                        origen,
                        destino,
                        producto,
                        new BigDecimal("10.000000"),
                        false,
                        solicitud
                );

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).lote()).isEqualTo(loteDestino);
        assertThat(resultado.get(0).cantidad()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(loteOrigen.getStockLote()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generaAtencionesParaDetallesDeDistintosLotesSinConflicto() {
        Almacen origen = new Almacen();
        origen.setId(20);

        Producto producto = new Producto();
        producto.setId(3000);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO);

        LoteProducto loteUno = new LoteProducto();
        loteUno.setId(4000L);
        loteUno.setProducto(producto);
        loteUno.setAlmacen(origen);

        LoteProducto loteDos = new LoteProducto();
        loteDos.setId(4001L);
        loteDos.setProducto(producto);
        loteDos.setAlmacen(origen);

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(5000L)
                .estado(EstadoSolicitudMovimiento.AUTORIZADA)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .almacenOrigen(origen)
                .detalles(new java.util.ArrayList<>())
                .build();

        SolicitudMovimientoDetalle detalleLoteUno = SolicitudMovimientoDetalle.builder()
                .id(5001L)
                .solicitudMovimiento(solicitud)
                .lote(loteUno)
                .cantidad(new BigDecimal("50.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
                .build();

        SolicitudMovimientoDetalle detalleLoteDos = SolicitudMovimientoDetalle.builder()
                .id(5002L)
                .solicitudMovimiento(solicitud)
                .lote(loteDos)
                .cantidad(new BigDecimal("60.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
                .build();

        solicitud.getDetalles().add(detalleLoteUno);
        solicitud.getDetalles().add(detalleLoteDos);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("110.000000"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                null,
                producto.getId(),
                loteUno.getId(),
                origen.getId(),
                null,
                null,
                null,
                null,
                null,
                solicitud.getId(),
                null,
                null,
                null,
                null,
                null,
                EstadoLote.DISPONIBLE,
                Boolean.FALSE,
                List.of()
        );

        List<AtencionDTO> generadas = (List<AtencionDTO>) ReflectionTestUtils.invokeMethod(
                service,
                "obtenerAtencionesParaSolicitud",
                dto,
                solicitud
        );

        assertThat(generadas).hasSize(2);
        assertThat(generadas).extracting(AtencionDTO::getDetalleId)
                .containsExactly(detalleLoteUno.getId(), detalleLoteDos.getId());
        assertThat(generadas).extracting(AtencionDTO::getLoteId)
                .containsExactly(loteUno.getId(), loteDos.getId());
        assertThat(generadas).extracting(AtencionDTO::getCantidad)
                .containsExactly(new BigDecimal("50.000000"), new BigDecimal("60.000000"));
    }

    @Test
    void atiendeSolicitudConMultiplesDetallesDelMismoLoteSinExcederPendiente() {
        Almacen origen = new Almacen();
        origen.setId(3);

        Producto producto = new Producto();
        producto.setId(500);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO);

        LoteProducto lote = new LoteProducto();
        lote.setId(600L);
        lote.setProducto(producto);
        lote.setAlmacen(origen);
        lote.setStockLote(new BigDecimal("150.00"));
        lote.setStockReservado(new BigDecimal("100.000000"));

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(700L)
                .estado(EstadoSolicitudMovimiento.AUTORIZADA)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(origen)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(new java.util.ArrayList<>())
                .build();

        SolicitudMovimientoDetalle detalle12 = SolicitudMovimientoDetalle.builder()
                .id(710L)
                .solicitudMovimiento(solicitud)
                .lote(lote)
                .cantidad(new BigDecimal("12.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
                .build();

        SolicitudMovimientoDetalle detalle88 = SolicitudMovimientoDetalle.builder()
                .id(711L)
                .solicitudMovimiento(solicitud)
                .lote(lote)
                .cantidad(new BigDecimal("88.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
                .build();

        solicitud.getDetalles().add(detalle12);
        solicitud.getDetalles().add(detalle88);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("100.000000"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                null,
                producto.getId(),
                lote.getId(),
                origen.getId(),
                null,
                null,
                null,
                null,
                null,
                123L,
                solicitud.getId(),
                null,
                null,
                null,
                null,
                null,
                EstadoLote.DISPONIBLE,
                Boolean.FALSE,
                List.of()
        );

        when(loteProductoRepository.findByIdForUpdate(lote.getId())).thenReturn(Optional.of(lote));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(solicitudMovimientoDetalleRepository.findById(detalle12.getId())).thenReturn(Optional.of(detalle12));
        when(solicitudMovimientoDetalleRepository.findById(detalle88.getId())).thenReturn(Optional.of(detalle88));
        when(solicitudMovimientoDetalleRepository.save(any(SolicitudMovimientoDetalle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaLoteRepository.sumPendienteActivaByLoteId(eq(lote.getId()), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(
                service,
                "atenderSolicitudMovimiento",
                dto,
                solicitud
        )).doesNotThrowAnyException();

        ArgumentCaptor<SolicitudMovimientoDetalle> detalleCaptor = ArgumentCaptor.forClass(SolicitudMovimientoDetalle.class);
        ArgumentCaptor<BigDecimal> cantidadCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        verify(reservaLoteService, times(2)).consumirReserva(
                eq(solicitud),
                detalleCaptor.capture(),
                eq(lote),
                cantidadCaptor.capture()
        );

        assertThat(detalleCaptor.getAllValues()).extracting(SolicitudMovimientoDetalle::getId)
                .containsExactly(detalle12.getId(), detalle88.getId());
        assertThat(cantidadCaptor.getAllValues())
                .containsExactly(new BigDecimal("12.000000"), new BigDecimal("88.000000"));
    }

    @Test
    void lanzaConflictoCuandoCantidadSolicitadaExcedePendienteTotal() {
        Almacen origen = new Almacen();
        origen.setId(8);

        Producto producto = new Producto();
        producto.setId(900);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO);

        LoteProducto lote = new LoteProducto();
        lote.setId(901L);
        lote.setProducto(producto);
        lote.setAlmacen(origen);
        lote.setCodigoLote("LOTE-EXCESO");

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(902L)
                .estado(EstadoSolicitudMovimiento.AUTORIZADA)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(origen)
                .detalles(new java.util.ArrayList<>())
                .build();

        SolicitudMovimientoDetalle detallePendiente60 = SolicitudMovimientoDetalle.builder()
                .id(903L)
                .solicitudMovimiento(solicitud)
                .lote(lote)
                .cantidad(new BigDecimal("60.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
                .build();

        SolicitudMovimientoDetalle detallePendiente40 = SolicitudMovimientoDetalle.builder()
                .id(904L)
                .solicitudMovimiento(solicitud)
                .lote(lote)
                .cantidad(new BigDecimal("40.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
                .build();

        solicitud.getDetalles().add(detallePendiente60);
        solicitud.getDetalles().add(detallePendiente40);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("150.000000"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                null,
                producto.getId(),
                lote.getId(),
                origen.getId(),
                null,
                null,
                null,
                null,
                null,
                solicitud.getId(),
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                EstadoLote.DISPONIBLE,
                Boolean.FALSE,
                List.of()
        );

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service,
                "obtenerAtencionesParaSolicitud",
                dto,
                solicitud
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException rse = (ResponseStatusException) exception;
                    assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(rse.getReason()).isEqualTo("ATENCION_CANTIDAD_EXCEDE_PENDIENTE");
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void salidaPt_autoSplitFefo_ok() {
        Long almacenPtId = 900L;

        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(1L);
        unidad.setNombre("Kilogramo");
        unidad.setSimbolo("KG");

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(2L);
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto producto = new Producto();
        producto.setId(10);
        producto.setUnidadMedida(unidad);
        producto.setCategoriaProducto(categoria);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("5.000"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "REF-PT",
                "Destino PT",
                producto.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                77L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Boolean.TRUE,
                null
        );

        MovimientoInventario movimientoBase = new MovimientoInventario();
        movimientoBase.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimientoBase.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        movimientoBase.setDocReferencia("REF-PT");

        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(77L);
        tipoDetalle.setDescripcion("Salida PT");

        Almacen almacenPt = new Almacen();
        almacenPt.setId(almacenPtId.intValue());
        almacenPt.setNombre("PT");

        LoteProducto lote1 = new LoteProducto();
        lote1.setId(1L);
        lote1.setCodigoLote("PT-001");
        lote1.setProducto(producto);
        lote1.setAlmacen(almacenPt);
        lote1.setEstado(EstadoLote.DISPONIBLE);
        lote1.setStockLote(new BigDecimal("3.000"));
        lote1.setStockReservado(BigDecimal.ZERO.setScale(6));

        LoteProducto lote2 = new LoteProducto();
        lote2.setId(2L);
        lote2.setCodigoLote("PT-002");
        lote2.setProducto(producto);
        lote2.setAlmacen(almacenPt);
        lote2.setEstado(EstadoLote.LIBERADO);
        lote2.setStockLote(new BigDecimal("4.000"));
        lote2.setStockReservado(new BigDecimal("1.000000"));

        Usuario usuario = Usuario.builder()
                .id(5L)
                .nombreUsuario("tester")
                .clave("pwd")
                .nombreCompleto("Tester")
                .correo("tester@example.com")
                .rol(RolUsuario.ROL_ALMACENISTA)
                .activo(true)
                .bloqueado(false)
                .build();

        when(movimientoInventarioMapper.toEntity(dto)).thenReturn(movimientoBase);
        when(productoRepository.findById(producto.getId().longValue())).thenReturn(Optional.of(producto));
        when(inventoryCatalogResolver.getTipoDetalleSalidaPtId()).thenReturn(77L);
        when(inventoryCatalogResolver.getTipoDetalleSalidaId()).thenReturn(20L);
        when(tipoMovimientoDetalleRepository.findById(77L)).thenReturn(Optional.of(tipoDetalle));
        when(inventoryCatalogResolver.getAlmacenPtId()).thenReturn(almacenPtId);
        when(inventoryCatalogResolver.decimals(unidad)).thenReturn(3);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(entityManager.getReference(Almacen.class, almacenPtId)).thenReturn(almacenPt);
        when(loteProductoRepository.findFefoSalidaPt(eq(producto.getId().longValue()), eq(almacenPtId), any()))
                .thenReturn(List.of(lote1, lote2));
        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lote1));
        when(loteProductoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(lote2));
        when(loteProductoRepository.save(any(LoteProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaLoteRepository.sumPendienteActivaByLoteId(anyLong(), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> {
                    MovimientoInventario mov = invocation.getArgument(0);
                    mov.setId(200L);
                    return mov;
                });
        when(movimientoInventarioRepository.saveAll(any(List.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoInventarioMapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> {
                    MovimientoInventario mov = invocation.getArgument(0);
                    return MovimientoInventarioResponseDTO.builder()
                            .id(mov.getId())
                            .cantidad(mov.getCantidad())
                            .build();
                });

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta.getId()).isEqualTo(200L);
        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());
        MovimientoInventario guardado = movimientoCaptor.getValue();
        assertThat(guardado.getAlmacenOrigen()).isEqualTo(almacenPt);
        assertThat(guardado.getCantidad()).isEqualByComparingTo(new BigDecimal("3.000"));

        ArgumentCaptor<List<MovimientoInventario>> adicionalesCaptor = ArgumentCaptor.forClass(List.class);
        verify(movimientoInventarioRepository).saveAll(adicionalesCaptor.capture());
        List<MovimientoInventario> adicionales = adicionalesCaptor.getValue();
        assertThat(adicionales).hasSize(1);
        assertThat(adicionales.get(0).getCantidad()).isEqualByComparingTo(new BigDecimal("2.000"));
        assertThat(adicionales.get(0).getAlmacenOrigen()).isEqualTo(almacenPt);

        verify(loteProductoRepository, times(2)).save(any(LoteProducto.class));
        assertThat(lote1.getStockLote()).isEqualByComparingTo(new BigDecimal("0.000"));
        assertThat(lote1.getStockLote().scale()).isEqualTo(3);
        assertThat(lote2.getStockLote()).isEqualByComparingTo(new BigDecimal("2.000"));
        assertThat(lote2.getStockLote().scale()).isEqualTo(3);
        assertThat(lote1.getStockReservado()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        assertThat(lote2.getStockReservado()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
    }

    @Test
    void salidaPt_rechazaEstadosNoElegibles() {
        Long almacenPtId = 901L;

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(3L);
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto producto = new Producto();
        producto.setId(11);
        producto.setCategoriaProducto(categoria);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.ONE,
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "DOC",
                "Destino",
                producto.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                77L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                null
        );

        AtencionDTO atencion = new AtencionDTO();
        atencion.setLoteId(15L);
        atencion.setCantidad(BigDecimal.ONE);

        Almacen almacenPt = new Almacen();
        almacenPt.setId(almacenPtId.intValue());

        LoteProducto lote = new LoteProducto();
        lote.setId(15L);
        lote.setProducto(producto);
        lote.setAlmacen(almacenPt);
        lote.setEstado(EstadoLote.EN_CUARENTENA);
        lote.setStockLote(new BigDecimal("2.000"));
        lote.setStockReservado(BigDecimal.ZERO.setScale(6));

        when(loteProductoRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service,
                "procesarSalidaPt",
                dto,
                producto,
                BigDecimal.ONE,
                almacenPtId,
                List.of(atencion),
                false
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException rse = (ResponseStatusException) exception;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).isEqualTo("LOTE_ESTADO_NO_ELEGIBLE");
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void salidaNoPt_regresion_ok() {
        Long almacenPtId = 902L;

        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(4L);
        unidad.setNombre("Kilogramo");
        unidad.setSimbolo("KG");

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(4L);
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto producto = new Producto();
        producto.setId(12);
        producto.setUnidadMedida(unidad);
        producto.setCategoriaProducto(categoria);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("2.000"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                "REF-GEN",
                null,
                producto.getId(),
                30L,
                321,
                null,
                null,
                null,
                null,
                20L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                null
        );

        MovimientoInventario movimientoBase = new MovimientoInventario();
        movimientoBase.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimientoBase.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        movimientoBase.setDocReferencia("REF-GEN");

        TipoMovimientoDetalle detalleGeneral = new TipoMovimientoDetalle();
        detalleGeneral.setId(20L);
        detalleGeneral.setDescripcion("Salida general");

        Usuario usuario = Usuario.builder()
                .id(6L)
                .nombreUsuario("operador")
                .clave("pwd")
                .nombreCompleto("Operador")
                .correo("operador@example.com")
                .rol(RolUsuario.ROL_ALMACENISTA)
                .activo(true)
                .bloqueado(false)
                .build();

        Almacen origen = new Almacen();
        origen.setId(321);
        origen.setNombre("Origen");

        LoteProducto lote = new LoteProducto();
        lote.setId(30L);
        lote.setCodigoLote("GEN-001");
        lote.setProducto(producto);
        lote.setAlmacen(origen);
        lote.setEstado(EstadoLote.DISPONIBLE);
        lote.setStockLote(new BigDecimal("5.000"));
        lote.setStockReservado(BigDecimal.ZERO.setScale(6));

        when(movimientoInventarioMapper.toEntity(dto)).thenReturn(movimientoBase);
        when(productoRepository.findById(producto.getId().longValue())).thenReturn(Optional.of(producto));
        when(inventoryCatalogResolver.getTipoDetalleSalidaPtId()).thenReturn(77L);
        when(inventoryCatalogResolver.getTipoDetalleSalidaId()).thenReturn(20L);
        when(inventoryCatalogResolver.getAlmacenPtId()).thenReturn(almacenPtId);
        when(tipoMovimientoDetalleRepository.findById(20L)).thenReturn(Optional.of(detalleGeneral));
        when(inventoryCatalogResolver.decimals(unidad)).thenReturn(3);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(entityManager.getReference(Almacen.class, dto.almacenOrigenId())).thenReturn(origen);
        when(loteProductoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(lote));
        when(loteProductoRepository.save(any(LoteProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaLoteRepository.sumPendienteActivaByLoteId(anyLong(), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> {
                    MovimientoInventario mov = invocation.getArgument(0);
                    mov.setId(300L);
                    return mov;
                });
        when(movimientoInventarioMapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> {
                    MovimientoInventario mov = invocation.getArgument(0);
                    return MovimientoInventarioResponseDTO.builder()
                            .id(mov.getId())
                            .cantidad(mov.getCantidad())
                            .build();
                });

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta.getId()).isEqualTo(300L);
        verify(entityManager).getReference(Almacen.class, dto.almacenOrigenId());
        verify(entityManager, never()).getReference(Almacen.class, almacenPtId);

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(captor.capture());
        MovimientoInventario guardado = captor.getValue();
        assertThat(guardado.getAlmacenOrigen()).isEqualTo(origen);
        assertThat(guardado.getCantidad()).isEqualByComparingTo(new BigDecimal("2.000"));

        assertThat(lote.getStockLote()).isEqualByComparingTo(new BigDecimal("3.000"));
        assertThat(lote.getStockLote().scale()).isEqualTo(3);
        assertThat(lote.getStockReservado()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        verify(movimientoInventarioRepository, never()).saveAll(any());
    }

    @Test
    void escalaPorUm_ok() {
        UnidadMedida um0 = new UnidadMedida();
        um0.setId(10L);
        Producto prod0 = new Producto();
        prod0.setId(40);
        prod0.setUnidadMedida(um0);
        LoteProducto lote0 = new LoteProducto();
        lote0.setId(100L);
        lote0.setProducto(prod0);
        lote0.setStockLote(new BigDecimal("10"));

        when(inventoryCatalogResolver.decimals(um0)).thenReturn(0);
        when(reservaLoteRepository.sumPendienteActivaByLoteId(eq(100L), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);

        ReflectionTestUtils.invokeMethod(service, "actualizarStockLote", lote0, new BigDecimal("3"), prod0);

        assertThat(lote0.getStockLote()).isEqualByComparingTo(new BigDecimal("7"));
        assertThat(lote0.getStockLote().scale()).isEqualTo(0);

        UnidadMedida um3 = new UnidadMedida();
        um3.setId(11L);
        Producto prod3 = new Producto();
        prod3.setId(41);
        prod3.setUnidadMedida(um3);
        LoteProducto lote3 = new LoteProducto();
        lote3.setId(101L);
        lote3.setProducto(prod3);
        lote3.setStockLote(new BigDecimal("5.555"));

        when(inventoryCatalogResolver.decimals(um3)).thenReturn(3);
        when(reservaLoteRepository.sumPendienteActivaByLoteId(eq(101L), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);

        ReflectionTestUtils.invokeMethod(service, "actualizarStockLote", lote3, new BigDecimal("1.111"), prod3);

        assertThat(lote3.getStockLote()).isEqualByComparingTo(new BigDecimal("4.444"));
        assertThat(lote3.getStockLote().scale()).isEqualTo(3);

        UnidadMedida um6 = new UnidadMedida();
        um6.setId(12L);
        Producto prod6 = new Producto();
        prod6.setId(42);
        prod6.setUnidadMedida(um6);
        LoteProducto lote6 = new LoteProducto();
        lote6.setId(102L);
        lote6.setProducto(prod6);
        lote6.setStockLote(new BigDecimal("2.000000"));

        when(inventoryCatalogResolver.decimals(um6)).thenReturn(6);
        when(reservaLoteRepository.sumPendienteActivaByLoteId(eq(102L), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);

        ReflectionTestUtils.invokeMethod(service, "actualizarStockLote", lote6, new BigDecimal("0.123456"), prod6);

        assertThat(lote6.getStockLote()).isEqualByComparingTo(new BigDecimal("1.876544"));
        assertThat(lote6.getStockLote().scale()).isEqualTo(6);
    }
}
