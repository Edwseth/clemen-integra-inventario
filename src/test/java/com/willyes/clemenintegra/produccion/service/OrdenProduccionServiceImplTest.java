package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.inventario.service.UmValidator;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.InsumoOPDTO;
import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenProduccionServiceImplTest {

    @Mock FormulaProductoRepository formulaProductoRepository;
    @Mock ProductoRepository productoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock SolicitudMovimientoService solicitudMovimientoService;
    @Mock OrdenProduccionRepository repository;
    @Mock MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock CierreProduccionRepository cierreProduccionRepository;
    @Mock MovimientoInventarioService movimientoInventarioService;
    @Mock LoteProductoRepository loteProductoRepository;
    @Mock AlmacenRepository almacenRepository;
    @Mock com.willyes.clemenintegra.produccion.service.UnidadConversionService unidadConversionService;
    @Mock EtapaProduccionRepository etapaProduccionRepository;
    @Mock EtapaPlantillaRepository etapaPlantillaRepository;
    @Mock MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock UsuarioService usuarioService;
    @Mock SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock InventoryCatalogResolver catalogResolver;
    @Mock StockQueryService stockQueryService;
    @Mock UmValidator umValidator;

    OrdenProduccionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrdenProduccionServiceImpl(
                formulaProductoRepository,
                productoRepository,
                stockQueryService,
                usuarioRepository,
                solicitudMovimientoService,
                repository,
                motivoMovimientoRepository,
                tipoMovimientoDetalleRepository,
                cierreProduccionRepository,
                movimientoInventarioService,
                loteProductoRepository,
                almacenRepository,
                unidadConversionService,
                etapaProduccionRepository,
                etapaPlantillaRepository,
                movimientoInventarioRepository,
                movimientoInventarioMapper,
                usuarioService,
                solicitudMovimientoRepository,
                catalogResolver,
                umValidator
        );

        when(umValidator.ajustar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(umValidator.getRoundingMode()).thenReturn(java.math.RoundingMode.HALF_UP);

        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE,AUTORIZADA,RESERVADA");
        ReflectionTestUtils.setField(service, "estadosSolicitudConcluyentesConf", "ATENDIDA,EJECUTADA,CANCELADA,RECHAZADO");

        when(catalogResolver.getAlmacenPtId()).thenReturn(2L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(catalogResolver.getMotivoIdEntradaPt()).thenReturn(11L);
        when(catalogResolver.getTipoDetalleEntradaId()).thenReturn(9L);
        when(catalogResolver.getMotivoIdDevolucionDesdeProduccion()).thenReturn(30L);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(8L);
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(null);
        when(catalogResolver.getAlmacenObsoletosId()).thenReturn(3L);
        when(catalogResolver.getMotivoIdTransferenciaCalidad()).thenReturn(12L);
        when(catalogResolver.getAlmacenBodegaPrincipalId()).thenReturn(1L);
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(5L);

        MotivoMovimiento motivoDev = new MotivoMovimiento();
        motivoDev.setId(30L);
        when(motivoMovimientoRepository.findById(30L)).thenReturn(Optional.of(motivoDev));
        when(solicitudMovimientoRepository.findWithDetalles(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(anyLong(), anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void listarCierresSortInvalido() {
        Pageable pageable = PageRequest.of(0,10, Sort.by("fecha"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.listarCierres(1L, pageable));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void registrarCierreOrdenFinalizada() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.FINALIZADA)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();
        assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
    }

    @Test
    void registrarCierreSinCantidadRestante() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.TEN)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();
        assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
    }

    @Test
    void registrarCierreConReservasPendientes() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));

        LoteProducto lote = LoteProducto.builder()
                .id(5L)
                .producto(Producto.builder().id(2).build())
                .build();
        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .id(20L)
                .lote(lote)
                .cantidad(new BigDecimal("5"))
                .build();
        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(10L)
                .detalles(List.of(detalle))
                .build();
        detalle.setSolicitudMovimiento(solicitud);

        when(solicitudMovimientoRepository.findWithDetalles(eq(1L), any(), any(), any()))
                .thenReturn(List.of(solicitud));
        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(eq(10L), eq(2L), eq(5L),
                eq(TipoMovimiento.SALIDA), eq(8L), isNull()))
                .thenReturn(new BigDecimal("3"));
        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(eq(10L), eq(2L), eq(5L),
                eq(TipoMovimiento.DEVOLUCION), isNull(), eq(30L)))
                .thenReturn(BigDecimal.ZERO);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registrarCierre(1L, dto));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertEquals("RESERVAS_PENDIENTES_OP", ex.getReason());
    }

    @Test
    void registrarCierreAsignaUsuarioYObservacion() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .loteProduccion("L1")
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNombreCompleto("Jane Doe");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any())).thenReturn(orden);
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(Almacen.builder().id(2L).build()));
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(Almacen.builder().id(7L).build()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(anyLong(), anyLong()))
                .thenReturn(Optional.of(LoteProducto.builder()
                        .id(1L)
                        .almacen(Almacen.builder().id(2L).build())
                        .estado(EstadoLote.DISPONIBLE)
                        .stockLote(BigDecimal.ZERO)
                        .build()));
        when(loteProductoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(movimientoInventarioRepository.existsByTipoMovimientoAndProductoIdAndLoteIdAndOrdenProduccionId(
                any(), anyLong(), anyLong(), anyLong())).thenReturn(false);
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(11L);
        when(motivoMovimientoRepository.findById(11L))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        when(movimientoInventarioRepository.existsByTipoMovimientoAndProductoIdAndLoteIdAndOrdenProduccionId(
                any(), anyLong(), anyLong(), anyLong())).thenReturn(false);
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(11L);
        when(motivoMovimientoRepository.findById(11L))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        when(movimientoInventarioRepository.existsByTipoMovimientoAndProductoIdAndLoteIdAndOrdenProduccionId(
                any(), anyLong(), anyLong(), anyLong())).thenReturn(false);
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(11L);
        when(motivoMovimientoRepository.findById(11L))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .observacion("obs")
                .build();

        service.registrarCierre(1L, dto);

        ArgumentCaptor<CierreProduccion> captor = ArgumentCaptor.forClass(CierreProduccion.class);
        verify(cierreProduccionRepository).save(captor.capture());
        CierreProduccion cierre = captor.getValue();
        assertEquals(usuario.getId(), cierre.getUsuarioId());
        assertEquals(usuario.getNombreCompleto(), cierre.getUsuarioNombre());
        assertEquals("obs", cierre.getObservacion());
    }


    @Test
    void registrarCierreTotalCompletoMarcaOrdenFinalizada() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .loteProduccion("L1")
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("Jane Doe");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(cierreProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(Almacen.builder().id(2L).build()));
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(Almacen.builder().id(7L).build()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(anyLong(), anyLong()))
                .thenReturn(Optional.of(LoteProducto.builder()
                        .id(1L)
                        .almacen(Almacen.builder().id(2L).build())
                        .estado(EstadoLote.DISPONIBLE)
                        .stockLote(BigDecimal.ZERO)
                        .build()));
        when(loteProductoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.TEN)
                .tipo(TipoCierre.TOTAL)
                .cerradaIncompleta(false)
                .build();

        service.registrarCierre(1L, dto);

        assertEquals(EstadoProduccion.FINALIZADA, orden.getEstado());
        verify(repository).save(orden);
    }

    @Test
    void registrarCierreTotalIncompletoMarcaOrdenCerradaIncompleta() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .loteProduccion("L1")
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("Jane Doe");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(cierreProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(Almacen.builder().id(2L).build()));
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(Almacen.builder().id(7L).build()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(anyLong(), anyLong()))
                .thenReturn(Optional.of(LoteProducto.builder()
                        .id(1L)
                        .almacen(Almacen.builder().id(2L).build())
                        .estado(EstadoLote.DISPONIBLE)
                        .stockLote(BigDecimal.ZERO)
                        .build()));
        when(loteProductoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.valueOf(5))
                .tipo(TipoCierre.TOTAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .cerradaIncompleta(true)
                .build();

        service.registrarCierre(1L, dto);

        assertEquals(EstadoProduccion.CERRADA_INCOMPLETA, orden.getEstado());
        verify(repository).save(orden);
    }

    @Test
    void registrarCierreRegistraMovimientoEntrada() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .loteProduccion("L1")
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("Jane Doe");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(cierreProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(Almacen.builder().id(2L).build()));
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(Almacen.builder().id(7L).build()));
        LoteProducto lote = LoteProducto.builder()
                .id(1L)
                .almacen(Almacen.builder().id(2L).build())
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(BigDecimal.ZERO)
                .build();
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(anyLong(), anyLong()))
                .thenReturn(Optional.of(lote));
        when(loteProductoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoInventarioRepository.findByTipoMovimientoAndMotivoMovimientoIdAndOrdenProduccionIdAndProductoIdAndLoteId(
                any(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(Optional.empty());
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(11L);
        when(motivoMovimientoRepository.findById(11L))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();

        service.registrarCierre(1L, dto);

        ArgumentCaptor<MovimientoInventarioDTO> movCaptor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(movCaptor.capture());
        MovimientoInventarioDTO mov = movCaptor.getValue();
        assertEquals(TipoMovimiento.ENTRADA, mov.tipoMovimiento());
        assertEquals(ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO, mov.clasificacionMovimientoInventario());
        assertEquals(1, mov.productoId());
        assertEquals(lote.getId(), mov.loteProductoId());
        assertEquals(2, mov.almacenDestinoId());
        assertEquals(motivo.getId(), mov.motivoMovimientoId());
        assertEquals(tipoDetalle.getId(), mov.tipoMovimientoDetalleId());
        assertEquals(usuario.getId(), mov.usuarioId());
        assertEquals(orden.getId(), mov.ordenProduccionId());
    }

    @Test
    void registrarCierreEntradaIdempotente() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .loteProduccion("L1")
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("Jane Doe");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(cierreProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(Almacen.builder().id(2L).build()));
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(Almacen.builder().id(7L).build()));
        LoteProducto lote = LoteProducto.builder()
                .id(1L)
                .almacen(Almacen.builder().id(2L).build())
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(BigDecimal.ZERO)
                .build();
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(anyLong(), anyLong()))
                .thenReturn(Optional.of(lote));
        when(loteProductoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(11L);
        when(motivoMovimientoRepository.findById(11L))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        MovimientoInventario existente = MovimientoInventario.builder()
                .id(33L)
                .cantidad(BigDecimal.ONE)
                .build();
        when(movimientoInventarioRepository.findByTipoMovimientoAndMotivoMovimientoIdAndOrdenProduccionIdAndProductoIdAndLoteId(
                any(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(Optional.of(existente));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();

        OrdenProduccion result = service.registrarCierre(1L, dto);
        assertEquals(BigDecimal.ZERO, result.getCantidadProducidaAcumulada());
        verify(movimientoInventarioService, never()).registrarMovimiento(any());
    }

    @Test
    void registrarCierreEntradaDuplicadaCantidadDiferente() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .loteProduccion("L1")
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("Jane Doe");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(cierreProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(Almacen.builder().id(2L).build()));
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(Almacen.builder().id(7L).build()));
        LoteProducto lote = LoteProducto.builder()
                .id(1L)
                .almacen(Almacen.builder().id(2L).build())
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(BigDecimal.ZERO)
                .build();
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(anyLong(), anyLong()))
                .thenReturn(Optional.of(lote));
        when(loteProductoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(11L);
        when(motivoMovimientoRepository.findById(11L))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        MovimientoInventario existente = MovimientoInventario.builder()
                .id(33L)
                .cantidad(new BigDecimal("2"))
                .build();
        when(movimientoInventarioRepository.findByTipoMovimientoAndMotivoMovimientoIdAndOrdenProduccionIdAndProductoIdAndLoteId(
                any(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(Optional.of(existente));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(movimientoInventarioService, never()).registrarMovimiento(any());
    }

    @Test
    void registrarCierreUmDecimalesExcedidos() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("1000.123"))
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertEquals("UM_DECIMALES_EXCEDIDOS", ex.getReason());
    }

    @Test
    void registrarCierreEscalaLoteExcedida() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("KG").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("100.129"))
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
        assertEquals("ESCALA_LOTE_EXCEDIDA", ex.getReason());
    }

    @Test
    void registrarCierrePrecisionLoteExcedida() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(new BigDecimal("100"))
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("123456789.12"))
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
        assertEquals("PRECISION_LOTE_EXCEDIDA", ex.getReason());
    }

    @Test
    void registrarCierrePrecisionMovExcedida() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(new BigDecimal("100"))
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("12345678.12"))
                .tipo(TipoCierre.PARCIAL)
                .fechaFabricacion(LocalDateTime.now().minusDays(1))
                .build();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
        assertEquals("PRECISION_MOV_EXCEDIDA", ex.getReason());
    }

    @Test
    void clonarEtapasParaOrdenCreaEtapasDesdePlantilla() {
        OrdenProduccion op = OrdenProduccion.builder().id(1L).build();
        List<EtapaPlantilla> plantilla = List.of(
                EtapaPlantilla.builder().nombre("E1").secuencia(1).build(),
                EtapaPlantilla.builder().nombre("E2").secuencia(2).build(),
                EtapaPlantilla.builder().nombre("E3").secuencia(3).build()
        );

        service.clonarEtapasParaOrden(op, plantilla);

        ArgumentCaptor<List<EtapaProduccion>> captor = ArgumentCaptor.forClass(List.class);
        verify(etapaProduccionRepository).saveAll(captor.capture());
        List<EtapaProduccion> guardadas = captor.getValue();
        assertEquals(3, guardadas.size());
        assertEquals(EstadoEtapa.PENDIENTE, guardadas.get(0).getEstado());
        assertEquals(op, guardadas.get(0).getOrdenProduccion());
    }

    @Test
    void iniciarEtapaCambiaEstadoYUsuario() {
        OrdenProduccion orden = OrdenProduccion.builder().id(1L).estado(EstadoProduccion.EN_PROCESO).build();
        EtapaProduccion etapa = EtapaProduccion.builder().id(2L).ordenProduccion(orden).estado(EstadoEtapa.PENDIENTE).build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EtapaProduccion result = service.iniciarEtapa(1L, 2L);

        assertEquals(EstadoEtapa.EN_PROCESO, result.getEstado());
        assertNotNull(result.getFechaInicio());
        assertEquals("John Doe", result.getUsuarioNombre());
    }


    @Test
    void iniciarPrimerEtapaActualizaEstadoOrden() {
        OrdenProduccion orden = OrdenProduccion.builder().id(1L).estado(EstadoProduccion.CREADA).build();
        EtapaProduccion etapa = EtapaProduccion.builder().id(2L).ordenProduccion(orden).estado(EstadoEtapa.PENDIENTE).secuencia(1).build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.iniciarEtapa(1L, 2L);

        assertEquals(EstadoProduccion.EN_PROCESO, orden.getEstado());
        verify(repository).save(orden);
    }

    @Test
    void iniciarEtapaSecuenciaUnoGeneraMovimientosPorInsumo() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.ONE)
                .producto(Producto.builder().id(10).build())
                .build();
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(2L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");

        Producto ins1 = Producto.builder().id(100).build();
        Producto ins2 = Producto.builder().id(200).build();
        DetalleFormula det1 = DetalleFormula.builder().insumo(ins1).cantidadNecesaria(BigDecimal.ONE).build();
        DetalleFormula det2 = DetalleFormula.builder().insumo(ins2).cantidadNecesaria(BigDecimal.valueOf(2)).build();
        FormulaProducto formula = FormulaProducto.builder().detalles(List.of(det1, det2)).build();

        LoteProducto lote1 = LoteProducto.builder().id(1L).stockLote(BigDecimal.ONE)
                .almacen(Almacen.builder().id(1L).build()).build();
        LoteProducto lote2 = LoteProducto.builder().id(2L).stockLote(BigDecimal.valueOf(2))
                .almacen(Almacen.builder().id(2L).build()).build();

        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(7L);
        TipoMovimientoDetalle detalle = new TipoMovimientoDetalle(); detalle.setId(8L); detalle.setDescripcion("SALIDA_PRODUCCION");

        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(movimientoInventarioRepository.existsByOrdenProduccionIdAndClasificacion(1L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(false);
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(Optional.of(motivo));
        when(tipoMovimientoDetalleRepository.findById(8L))
                .thenReturn(Optional.of(detalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());
        when(solicitudMovimientoRepository.findWithDetalles(1L, null, null, null)).thenReturn(List.of());
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.iniciarEtapa(1L, 2L);

        ArgumentCaptor<MovimientoInventarioDTO> movCaptor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService, times(2)).registrarMovimiento(movCaptor.capture());
        List<MovimientoInventarioDTO> movs = movCaptor.getAllValues();
        assertEquals(BigDecimal.ONE, movs.get(0).cantidad());
        assertEquals(BigDecimal.valueOf(2), movs.get(1).cantidad());
        assertTrue(movs.stream().allMatch(m -> m.ordenProduccionId().equals(1L)));
        assertTrue(movs.stream().allMatch(m -> m.usuarioId().equals(5L)));
        assertTrue(movs.stream().allMatch(m -> m.tipoMovimiento() == TipoMovimiento.SALIDA));
        assertTrue(movs.stream().allMatch(m -> m.clasificacionMovimientoInventario() == ClasificacionMovimientoInventario.SALIDA_PRODUCCION));
    }

    @Test
    void iniciarEtapaSecuenciaMayorNoConsume() {
        OrdenProduccion orden = OrdenProduccion.builder().id(1L).estado(EstadoProduccion.EN_PROCESO).build();
        EtapaProduccion etapa = EtapaProduccion.builder().id(2L).ordenProduccion(orden).estado(EstadoEtapa.PENDIENTE).secuencia(2).build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.iniciarEtapa(1L, 2L);

        verify(movimientoInventarioService, never()).registrarMovimiento(any());
    }

    @Test
    void iniciarEtapaIdempotenteNoRepiteConsumo() {
        OrdenProduccion orden = OrdenProduccion.builder().id(1L).estado(EstadoProduccion.EN_PROCESO).producto(Producto.builder().id(10).build()).build();
        EtapaProduccion etapa = EtapaProduccion.builder().id(2L).ordenProduccion(orden).estado(EstadoEtapa.PENDIENTE).secuencia(1).build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(movimientoInventarioRepository.existsByOrdenProduccionIdAndClasificacion(1L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(true);
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.iniciarEtapa(1L, 2L);

        verify(movimientoInventarioService, never()).registrarMovimiento(any());
    }

    @Test
    void iniciarEtapaStockInsuficienteRevierte() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .producto(Producto.builder().id(10).build())
                .build();
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(2L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");
        Producto insumo = Producto.builder().id(100).build();
        DetalleFormula det = DetalleFormula.builder().insumo(insumo).cantidadNecesaria(BigDecimal.ONE).build();
        FormulaProducto formula = FormulaProducto.builder().detalles(List.of(det)).build();
        LoteProducto lote = LoteProducto.builder().id(1L).stockLote(BigDecimal.valueOf(5))
                .almacen(Almacen.builder().id(1L).build()).build();
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(7L);
        TipoMovimientoDetalle detalle = new TipoMovimientoDetalle(); detalle.setId(8L); detalle.setDescripcion("SALIDA_PRODUCCION");

        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(movimientoInventarioRepository.existsByOrdenProduccionIdAndClasificacion(1L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(false);
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(Optional.of(motivo));
        when(tipoMovimientoDetalleRepository.findById(8L))
                .thenReturn(Optional.of(detalle));

        assertThrows(ResponseStatusException.class, () -> service.iniciarEtapa(1L, 2L));
        verify(movimientoInventarioService, never()).registrarMovimiento(any());
        assertEquals(EstadoEtapa.PENDIENTE, etapa.getEstado());
    }

    @Test
    void listarInsumosConsultaDetalleSalida() {
        Producto productoFinal = Producto.builder().id(1).build();
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .producto(productoFinal)
                .cantidadProgramada(BigDecimal.ONE)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));

        Producto insumo = Producto.builder()
                .id(2)
                .nombre("Insumo")
                .unidadMedida(UnidadMedida.builder().nombre("kg").build())
                .build();
        DetalleFormula det = DetalleFormula.builder()
                .insumo(insumo)
                .cantidadNecesaria(BigDecimal.ONE)
                .build();
        FormulaProducto formula = FormulaProducto.builder()
                .producto(productoFinal)
                .detalles(List.of(det))
                .build();
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(productoFinal.getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        TipoMovimientoDetalle detalleSalida = new TipoMovimientoDetalle();
        detalleSalida.setId(8L);
        detalleSalida.setDescripcion("SALIDA_PRODUCCION");
        when(tipoMovimientoDetalleRepository.findById(8L))
                .thenReturn(Optional.of(detalleSalida));

        when(movimientoInventarioRepository.sumaCantidadPorOrdenYProducto(1L, 2L, 8L))
                .thenReturn(new BigDecimal("0"));

        List<InsumoOPDTO> resultado = service.listarInsumos(1L);

        assertEquals(1, resultado.size());
        verify(movimientoInventarioRepository).sumaCantidadPorOrdenYProducto(1L, 2L, 8L);
    }

    @Test
    void finalizarEtapaMarcaOrdenFinalizadaSiTodasFinalizadas() {
        OrdenProduccion orden = OrdenProduccion.builder().id(1L).estado(EstadoProduccion.EN_PROCESO).build();
        EtapaProduccion etapa1 = EtapaProduccion.builder().id(1L).ordenProduccion(orden).estado(EstadoEtapa.FINALIZADA).build();
        EtapaProduccion etapa2 = EtapaProduccion.builder().id(2L).ordenProduccion(orden).estado(EstadoEtapa.EN_PROCESO).build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa2));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(1L))
                .thenReturn(List.of(etapa1, etapa2));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EtapaProduccion result = service.finalizarEtapa(1L, 2L, 5L);

        assertEquals(EstadoEtapa.FINALIZADA, result.getEstado());
        assertNotNull(result.getFechaFin());
        verify(repository).save(orden);
        assertEquals(EstadoProduccion.FINALIZADA, orden.getEstado());
    }

    @Test
    void crearOrdenInicializaCantidadesEnCero() {
        OrdenProduccionRequestDTO dto = OrdenProduccionRequestDTO.builder()
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(1))
                .cantidadProgramada(BigDecimal.TEN)
                .estado("CREADA")
                .productoId(1L)
                .responsableId(2L)
                .unidadMedidaSimbolo("kg")
                .build();

        Producto producto = new Producto();
        producto.setId(1);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("kg");
        producto.setUnidadMedida(unidad);
        producto.setRendimientoUnidad(BigDecimal.ONE);

        Usuario responsable = new Usuario();
        responsable.setId(2L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(responsable));
        when(unidadConversionService.convertir(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(unidadConversionService.dividirNormalizado(any(), any(), any(), any())).thenReturn(BigDecimal.TEN);

        OrdenProduccionServiceImpl spyService = spy(service);

        ArgumentCaptor<OrdenProduccion> captor = ArgumentCaptor.forClass(OrdenProduccion.class);
        doReturn(ResultadoValidacionOrdenDTO.builder().esValida(true).orden(new OrdenProduccionResponseDTO()).build())
                .when(spyService).guardarConValidacionStock(captor.capture());

        spyService.crearOrden(dto);

        OrdenProduccion capturada = captor.getValue();
        assertEquals(BigDecimal.ZERO, capturada.getCantidadProducida());
        assertEquals(BigDecimal.ZERO, capturada.getCantidadProducidaAcumulada());
    }
}
