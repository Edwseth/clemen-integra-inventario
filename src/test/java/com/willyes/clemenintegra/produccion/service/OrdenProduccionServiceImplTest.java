package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    OrdenProduccionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrdenProduccionServiceImpl(
                formulaProductoRepository,
                productoRepository,
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
                usuarioService
        );
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
                .build();
        assertThrows(ResponseStatusException.class, () -> service.registrarCierre(1L, dto));
    }

    @Test
    void registrarCierreAsignaUsuarioYObservacion() {
        OrdenProduccion orden = OrdenProduccion.builder()
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(orden));
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNombreCompleto("Jane Doe");
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any())).thenReturn(orden);

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
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(etapaProduccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EtapaProduccion result = service.iniciarEtapa(1L, 2L, 5L);

        assertEquals(EstadoEtapa.EN_PROCESO, result.getEstado());
        assertNotNull(result.getFechaInicio());
        assertEquals("John Doe", result.getUsuarioNombre());
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
                .loteProduccion("L1")
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
