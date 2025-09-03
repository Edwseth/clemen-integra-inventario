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
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.model.CierreProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
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
}
