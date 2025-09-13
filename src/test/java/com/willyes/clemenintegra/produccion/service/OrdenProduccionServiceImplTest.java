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
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
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
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.InsumoOPDTO;
import com.willyes.clemenintegra.produccion.dto.LoteProductoResponse;
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
import java.util.Map;
import java.util.Collections;

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
    @Mock VidaUtilProductoRepository vidaUtilProductoRepository;

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
                umValidator,
                vidaUtilProductoRepository
        );

        when(umValidator.ajustar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(umValidator.getRoundingMode()).thenReturn(java.math.RoundingMode.HALF_UP);

        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE,AUTORIZADA,RESERVADA");
        ReflectionTestUtils.setField(service, "estadosSolicitudConcluyentesConf", "ATENDIDA,EJECUTADA,CANCELADA,RECHAZADO");
        ReflectionTestUtils.setField(service, "clasificacionEntradaPtConf", "ENTRADA_PRODUCTO_TERMINADO");

        when(catalogResolver.getAlmacenPtId()).thenReturn(2L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(catalogResolver.getMotivoIdEntradaProductoTerminado()).thenReturn(11L);
        when(catalogResolver.getTipoDetalleEntradaId()).thenReturn(9L);
        when(catalogResolver.getMotivoIdDevolucionDesdeProduccion()).thenReturn(30L);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(8L);
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(null);
        when(catalogResolver.getAlmacenObsoletosId()).thenReturn(3L);
        when(catalogResolver.getMotivoIdTransferenciaCalidad()).thenReturn(12L);
        when(catalogResolver.getAlmacenBodegaPrincipalId()).thenReturn(1L);
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(5L);

        when(almacenRepository.findById(2L)).thenReturn(Optional.of(Almacen.builder().id(2L).build()));
        when(almacenRepository.findById(7L)).thenReturn(Optional.of(Almacen.builder().id(7L).build()));
        when(loteProductoRepository.save(any())).thenAnswer(inv -> {
            LoteProducto l = inv.getArgument(0);
            if (l.getId() == null) l.setId(1L);
            return l;
        });

        MotivoMovimiento motivoDev = new MotivoMovimiento();
        motivoDev.setId(30L);
        when(motivoMovimientoRepository.findById(30L)).thenReturn(Optional.of(motivoDev));
        when(solicitudMovimientoRepository.findWithDetalles(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(anyLong(), anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        LocalDateTime defaultFab = LocalDateTime.now().minusDays(1);
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(anyLong()))
                .thenReturn(List.of(EtapaProduccion.builder()
                        .secuencia(1)
                        .estado(EstadoEtapa.FINALIZADA)
                        .fechaInicio(defaultFab)
                        .build()));
        when(vidaUtilProductoRepository.findById(anyInt())).thenReturn(Optional.empty());
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
    void registrarCierreTotalIncompletoMarcaOrdenFinalizada() {
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
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(1L))
                .thenReturn(List.of(EtapaProduccion.builder().estado(EstadoEtapa.FINALIZADA).build()));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.valueOf(5))
                .tipo(TipoCierre.TOTAL)
                .cerradaIncompleta(true)
                .build();

        service.registrarCierre(1L, dto);

        assertEquals(EstadoProduccion.FINALIZADA, orden.getEstado());
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
    void registrarCierreIncrementaStockLoteUnaVez() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
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
                .thenReturn(Optional.empty());
        when(loteProductoRepository.findByCodigoLote("LOTE1")).thenReturn(Optional.empty());

        java.util.concurrent.atomic.AtomicReference<LoteProducto> loteRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(loteProductoRepository.save(any())).thenAnswer(inv -> {
            LoteProducto l = inv.getArgument(0);
            if (l.getId() == null) l.setId(1L);
            loteRef.set(l);
            return l;
        });

        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(11L);
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));

        doAnswer(inv -> {
            MovimientoInventarioDTO mov = inv.getArgument(0);
            LoteProducto l = loteRef.get();
            l.setStockLote(l.getStockLote().add(mov.cantidad()));
            return new MovimientoInventarioResponseDTO();
        }).when(movimientoInventarioService).registrarMovimiento(any());

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("3"))
                .tipo(TipoCierre.PARCIAL)
                .codigoLote("LOTE1")
                .build();

        service.registrarCierre(1L, dto);

        assertNotNull(loteRef.get());
        assertEquals(new BigDecimal("3.00"), loteRef.get().getStockLote());
    }

    @Test
    void entradaPtSinOrdenDevuelve422() {
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
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioRepository.findByTipoMovimientoAndMotivoMovimientoIdAndOrdenProduccionIdAndProductoIdAndLoteId(
                any(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(Optional.empty());
        when(movimientoInventarioService.registrarMovimiento(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ENTRADA_PT_REQUIERE_ORDEN_PRODUCCION_ID"));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertEquals("ENTRADA_PT_REQUIERE_ORDEN_PRODUCCION_ID", ex.getReason());
    }

    @Test
    void entradaPtConOrdenPasa() {
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
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioRepository.findByTipoMovimientoAndMotivoMovimientoIdAndOrdenProduccionIdAndProductoIdAndLoteId(
                any(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(Optional.empty());
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.PARCIAL)
                .build();

        service.registrarCierre(1L, dto);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture());
        assertEquals(1L, captor.getValue().ordenProduccionId());
        assertEquals(TipoMovimiento.ENTRADA, captor.getValue().tipoMovimiento());
    }

    @Test
    void variosCierresParcialesAcumulan() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(new BigDecimal("1000"))
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
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        for (BigDecimal qty : List.of(new BigDecimal("400"), new BigDecimal("300"), new BigDecimal("200"), new BigDecimal("100"))) {
            CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                    .cantidad(qty)
                    .tipo(TipoCierre.PARCIAL)
                    .build();
            service.registrarCierre(1L, dto);
        }

        assertEquals(new BigDecimal("1000"), orden.getCantidadProducidaAcumulada());
        assertEquals(new BigDecimal("1000"), lote.getStockLote());
        verify(movimientoInventarioService, times(4)).registrarMovimiento(any());
    }

    @Test
    void cierreTotalSobreproduccionFinalizaOrden() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(new BigDecimal("1000"))
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
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("1100"))
                .tipo(TipoCierre.TOTAL)
                .build();

        service.registrarCierre(1L, dto);

        assertEquals(new BigDecimal("1100"), orden.getCantidadProducidaAcumulada());
        assertEquals(EstadoProduccion.FINALIZADA, orden.getEstado());
        assertEquals(new BigDecimal("1100"), lote.getStockLote());
    }

    @Test
    void cierreTotalSubproduccionFinalizaOrden() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(new BigDecimal("1000"))
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
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("800"))
                .tipo(TipoCierre.TOTAL)
                .build();

        service.registrarCierre(1L, dto);

        assertEquals(new BigDecimal("800"), orden.getCantidadProducidaAcumulada());
        assertEquals(EstadoProduccion.FINALIZADA, orden.getEstado());
        assertEquals(new BigDecimal("800"), lote.getStockLote());
    }

    @Test
    void registrarCierreUsaFechaDePrimeraEtapa() {
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
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(new MotivoMovimiento()));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        LocalDateTime fechaEtapa1 = LocalDateTime.now().minusDays(3);
        EtapaProduccion etapa1 = EtapaProduccion.builder()
                .secuencia(1).estado(EstadoEtapa.FINALIZADA).fechaInicio(fechaEtapa1).build();
        EtapaProduccion etapa2 = EtapaProduccion.builder()
                .secuencia(2).estado(EstadoEtapa.FINALIZADA).fechaInicio(fechaEtapa1.plusDays(1)).build();
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(1L))
                .thenReturn(List.of(etapa1, etapa2));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.TOTAL)
                .build();

        service.registrarCierre(1L, dto);

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository).save(loteCaptor.capture());
        assertEquals(fechaEtapa1, loteCaptor.getValue().getFechaFabricacion());
    }

    @Test
    void registrarCierreCalculaVencimientoSegunVidaUtil() {
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
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(new MotivoMovimiento()));
        TipoMovimientoDetalle tipoDetalle2 = new TipoMovimientoDetalle(); tipoDetalle2.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle2));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        LocalDateTime fechaEtapa1 = LocalDateTime.now().minusDays(5);
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(1L))
                .thenReturn(List.of(EtapaProduccion.builder()
                        .secuencia(1)
                        .estado(EstadoEtapa.FINALIZADA)
                        .fechaInicio(fechaEtapa1)
                        .build()));
        VidaUtilProducto vida = VidaUtilProducto.builder().productoId(1).semanasVigencia(4).build();
        when(vidaUtilProductoRepository.findById(1)).thenReturn(Optional.of(vida));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.ONE)
                .tipo(TipoCierre.TOTAL)
                .build();

        service.registrarCierre(1L, dto);

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository).save(loteCaptor.capture());
        assertEquals(fechaEtapa1.plusWeeks(4), loteCaptor.getValue().getFechaVencimiento());
    }

    @Test
    void cierresGeneranMovimientosIndependientes() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(new BigDecimal("1000"))
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
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle(); tipoDetalle.setId(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());

        CierreProduccionRequestDTO d1 = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("600"))
                .tipo(TipoCierre.PARCIAL)
                .build();
        CierreProduccionRequestDTO d2 = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("400"))
                .tipo(TipoCierre.PARCIAL)
                .build();

        ArgumentCaptor<MovimientoInventarioDTO> movCaptor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        service.registrarCierre(1L, d1);
        service.registrarCierre(1L, d2);

        verify(movimientoInventarioService, times(2)).registrarMovimiento(movCaptor.capture());
        List<MovimientoInventarioDTO> movimientos = movCaptor.getAllValues();
        assertEquals(new BigDecimal("600"), movimientos.get(0).cantidad());
        assertEquals(new BigDecimal("400"), movimientos.get(1).cantidad());
        assertEquals(new BigDecimal("1000"), orden.getCantidadProducidaAcumulada());
        assertEquals(new BigDecimal("1000"), lote.getStockLote());
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
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.CREADA)
                .producto(Producto.builder().id(10).tipoAnalisis(TipoAnalisisCalidad.NINGUNO).build())
                .build();
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
                .producto(Producto.builder().id(10).tipoAnalisis(TipoAnalisisCalidad.NINGUNO).build())
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
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(1L)
                .estado(EstadoProduccion.EN_PROCESO)
                .producto(Producto.builder().id(10).tipoAnalisis(TipoAnalisisCalidad.NINGUNO).build())
                .build();
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
                .producto(Producto.builder().id(10).tipoAnalisis(TipoAnalisisCalidad.NINGUNO).build())
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
    void finalizarUltimaEtapaNoFinalizaOrden() {
        OrdenProduccion orden = OrdenProduccion.builder().id(1L).estado(EstadoProduccion.EN_PROCESO).build();
        EtapaProduccion etapa1 = EtapaProduccion.builder()
                .id(1L).ordenProduccion(orden)
                .estado(EstadoEtapa.FINALIZADA)
                .fechaInicio(LocalDateTime.now().minusDays(2))
                .build();
        EtapaProduccion etapa2 = EtapaProduccion.builder()
                .id(2L).ordenProduccion(orden)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa2));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(1L))
                .thenReturn(List.of(etapa1, etapa2));

        EtapaProduccion result = service.finalizarEtapa(1L, 2L, 5L);

        assertEquals(EstadoEtapa.FINALIZADA, result.getEstado());
        assertNotNull(result.getFechaFin());
        verify(repository, never()).save(any());
        assertEquals(EstadoProduccion.EN_PROCESO, orden.getEstado());
    }

    @Test
    void finalizarEtapasYRegistrarCierreTotalFinalizaOrden() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .loteProduccion("L1")
                .producto(Producto.builder().id(1).tipoAnalisis(TipoAnalisisCalidad.NINGUNO)
                        .unidadMedida(UnidadMedida.builder().simbolo("G").build()).build())
                .build();
        EtapaProduccion etapa1 = EtapaProduccion.builder()
                .id(1L).ordenProduccion(orden)
                .estado(EstadoEtapa.FINALIZADA)
                .fechaInicio(LocalDateTime.now().minusDays(2))
                .build();
        EtapaProduccion etapa2 = EtapaProduccion.builder()
                .id(2L).ordenProduccion(orden)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .build();
        Usuario usuario = new Usuario(); usuario.setId(5L); usuario.setNombreCompleto("John Doe");
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(2L)).thenReturn(Optional.of(etapa2));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(1L))
                .thenReturn(List.of(etapa1, etapa2));

        service.finalizarEtapa(1L, 2L, 5L);

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
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(1L))
                .thenReturn(List.of(etapa1, etapa2));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(BigDecimal.TEN)
                .tipo(TipoCierre.TOTAL)
                .build();

        service.registrarCierre(1L, dto);

        assertEquals(EstadoProduccion.FINALIZADA, orden.getEstado());
        verify(repository).save(orden);
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

    @Test
    void guardarConValidacionStockConStockSuficiente() {
        Producto producto = new Producto();
        producto.setId(1);

        Usuario responsable = new Usuario();
        responsable.setId(2L);

        OrdenProduccion orden = OrdenProduccion.builder()
                .producto(producto)
                .cantidadProgramada(BigDecimal.valueOf(5))
                .responsable(responsable)
                .build();

        Producto insumo = new Producto();
        insumo.setId(100);
        insumo.setNombre("Insumo A");
        UnidadMedida um = new UnidadMedida();
        um.setSimbolo("kg");
        insumo.setUnidadMedida(um);

        DetalleFormula detalle = DetalleFormula.builder()
                .insumo(insumo)
                .cantidadNecesaria(BigDecimal.valueOf(2))
                .build();

        FormulaProducto formula = FormulaProducto.builder()
                .detalles(List.of(detalle))
                .build();

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(1L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(List.of(100L))).thenReturn(List.of(insumo));
        when(catalogResolver.getAlmacenBodegaPrincipalId()).thenReturn(1L);
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(2L);
        when(stockQueryService.obtenerStockDisponible(eq(List.of(100L)), eq(List.of(1L, 2L))))
                .thenReturn(Map.of(100L, BigDecimal.valueOf(15)));
        when(repository.save(any())).thenAnswer(inv -> {
            OrdenProduccion o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });
        when(repository.countByCodigoOrdenStartingWith(anyString())).thenReturn(0L);
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(1L);
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(2L);
        when(tipoMovimientoDetalleRepository.findById(anyLong())).thenReturn(Optional.of(tipoDetalle));
        doNothing().when(solicitudMovimientoService).registrarSolicitud(any());

        OrdenProduccionServiceImpl spyService = spy(service);
        doReturn(List.of()).when(spyService).cargarPlantillaEtapas(any());
        doNothing().when(spyService).reservarInsumosParaOP(anyLong());

        ResultadoValidacionOrdenDTO resultado = spyService.guardarConValidacionStock(orden);

        assertTrue(resultado.isEsValida());
        verify(stockQueryService, times(1)).obtenerStockDisponible(eq(List.of(100L)), eq(List.of(1L, 2L)));
        verify(stockQueryService, never()).obtenerStockDisponible(anyLong());
    }

    @Test
    void guardarConValidacionStockSinStockEnAlmacenesPermitidos() {
        Producto producto = new Producto();
        producto.setId(1);

        Usuario responsable = new Usuario();
        responsable.setId(2L);

        OrdenProduccion orden = OrdenProduccion.builder()
                .producto(producto)
                .cantidadProgramada(BigDecimal.valueOf(5))
                .responsable(responsable)
                .build();

        Producto insumo = new Producto();
        insumo.setId(100);
        insumo.setNombre("Insumo A");
        UnidadMedida um = new UnidadMedida();
        um.setSimbolo("kg");
        insumo.setUnidadMedida(um);

        DetalleFormula detalle = DetalleFormula.builder()
                .insumo(insumo)
                .cantidadNecesaria(BigDecimal.valueOf(2))
                .build();

        FormulaProducto formula = FormulaProducto.builder()
                .detalles(List.of(detalle))
                .build();

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(1L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(List.of(100L))).thenReturn(List.of(insumo));
        when(catalogResolver.getAlmacenBodegaPrincipalId()).thenReturn(1L);
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(2L);
        when(stockQueryService.obtenerStockDisponible(eq(List.of(100L)), eq(List.of(1L, 2L))))
                .thenReturn(Collections.emptyMap());

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertFalse(resultado.isEsValida());
        assertEquals("Stock insuficiente para algunos insumos", resultado.getMensaje());
    }

    @Test
    void obtenerLote_ok() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(5L)
                .producto(Producto.builder().id(10).build())
                .build();
        when(repository.findById(5L)).thenReturn(Optional.of(orden));

        Almacen almacen = Almacen.builder()
                .id(1)
                .nombre("A1")
                .ubicacion("U")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();
        LoteProducto lote = LoteProducto.builder()
                .id(7L)
                .codigoLote("L1")
                .estado(EstadoLote.DISPONIBLE)
                .almacen(almacen)
                .build();
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(5L, 10L))
                .thenReturn(Optional.of(lote));

        LoteProductoResponse dto = service.obtenerLote(5L);

        assertNotNull(dto);
        assertEquals(7L, dto.getId());
        assertEquals("L1", dto.getCodigoLote());
        assertEquals(EstadoLote.DISPONIBLE, dto.getEstado());
        assertEquals(1, dto.getAlmacen().getId());
    }
}
