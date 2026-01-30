package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.BitacoraCambiosInventarioService;
import com.willyes.clemenintegra.inventario.repository.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraService;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.repository.RecepcionOCService;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteService;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.inventario.service.LoteCalidadValidator;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimientoInventarioServiceVencimientosTest {

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

    @Test
    void registrarRetiroPorVencimientoUsaUsuarioSistemaCuandoNoHayAuth() {
        LoteProducto lote = crearLote();
        InventoryVencidosProperties properties = crearProperties();
        Usuario usuarioSistema = Usuario.builder()
                .id(53L)
                .nombreUsuario("SYSTEM")
                .nombreCompleto("Sistema")
                .activo(true)
                .build();

        when(usuarioService.obtenerUsuarioAutenticado())
                .thenThrow(new AuthenticationCredentialsNotFoundException("No auth"));
        when(usuarioService.obtenerUsuarioSistemaJobVencimientos()).thenReturn(usuarioSistema);

        prepararReferencias(properties.getMovimiento().getMotivoId(), properties.getAlmacenDestinoId());
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(10L);
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventario movimiento = service.registrarRetiroPorVencimiento(lote, properties, LocalDateTime.now());

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());
        assertThat(movimiento).isNotNull();
        assertThat(movimientoCaptor.getValue().getRegistradoPor()).isSameAs(usuarioSistema);
        verify(usuarioService).obtenerUsuarioSistemaJobVencimientos();
    }

    @Test
    void registrarRetiroPorVencimientoFallaSiUsuarioSistemaNoExiste() {
        LoteProducto lote = crearLote();
        InventoryVencidosProperties properties = crearProperties();

        when(usuarioService.obtenerUsuarioAutenticado())
                .thenThrow(new AuthenticationCredentialsNotFoundException("No auth"));
        when(usuarioService.obtenerUsuarioSistemaJobVencimientos())
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USUARIO_SISTEMA_NO_CONFIGURADO"));

        prepararReferencias(properties.getMovimiento().getMotivoId(), properties.getAlmacenDestinoId());
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(10L);

        assertThatThrownBy(() -> service.registrarRetiroPorVencimiento(lote, properties, LocalDateTime.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("USUARIO_SISTEMA_NO_CONFIGURADO");
    }

    private LoteProducto crearLote() {
        Producto producto = new Producto();
        producto.setId(10);
        Almacen origen = new Almacen();
        origen.setId(1);

        return LoteProducto.builder()
                .id(5L)
                .producto(producto)
                .almacen(origen)
                .stockLote(new BigDecimal("12.50"))
                .build();
    }

    private InventoryVencidosProperties crearProperties() {
        InventoryVencidosProperties properties = new InventoryVencidosProperties();
        properties.getMovimiento().setEnabled(true);
        properties.getMovimiento().setMotivoId(20L);
        properties.getMovimiento().setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL.name());
        properties.setAlmacenDestinoId(2L);
        return properties;
    }

    private void prepararReferencias(Long motivoId, Long destinoId) {
        MotivoMovimiento motivoMovimiento = new MotivoMovimiento();
        motivoMovimiento.setId(motivoId);
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(10L);
        Almacen destino = new Almacen();
        destino.setId(Math.toIntExact(destinoId));

        given(entityManager.getReference(eq(MotivoMovimiento.class), eq(motivoId)))
                .willReturn(motivoMovimiento);
        given(entityManager.getReference(eq(TipoMovimientoDetalle.class), eq(10L)))
                .willReturn(tipoDetalle);
        given(entityManager.getReference(eq(Almacen.class), eq(Math.toIntExact(destinoId))))
                .willReturn(destino);
    }
}
