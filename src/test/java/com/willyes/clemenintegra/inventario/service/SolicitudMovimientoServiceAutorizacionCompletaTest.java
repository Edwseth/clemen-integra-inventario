package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudMovimientoServiceAutorizacionCompletaTest {

    @Mock
    private SolicitudMovimientoRepository repository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private ReservaLoteService reservaLoteService;

    @InjectMocks
    private SolicitudMovimientoServiceImpl service;

    private static final AtomicLong DETALLE_ID = new AtomicLong(1);

    @Test
    @DisplayName("autorizarSolicitudCompleta aprueba todos los detalles pendientes")
    void autorizarSolicitudCompleta_casoFeliz() {
        SolicitudMovimiento solicitud = crearSolicitudConDetalles(List.of(
                crearDetalle("L-1", "Insumo 1"),
                crearDetalle("L-2", "Insumo 2"),
                crearDetalle("L-3", "Insumo 3")
        ));

        Usuario responsable = new Usuario();
        responsable.setId(10L);

        ArgumentCaptor<SolicitudMovimiento> captor = ArgumentCaptor.forClass(SolicitudMovimiento.class);

        when(repository.findByIdWithLock(1L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(responsable));
        when(repository.saveAndFlush(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        mockAutenticacion();

        assertThat(solicitud.getDetalles()).hasSize(3);
        var respuesta = service.autorizarSolicitudCompleta(1L, 10L);

        assertThat(respuesta.getEstado()).isEqualTo(EstadoSolicitudMovimiento.AUTORIZADA);
        assertThat(respuesta.getFechaResolucion()).isNotNull();
        verify(reservaLoteService, times(3)).crearOActualizarDesdeDetalle(any());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoSolicitudMovimiento.AUTORIZADA);
    }

    @Test
    @DisplayName("autorizarSolicitudCompleta marca PARCIAL cuando algún detalle falla")
    void autorizarSolicitudCompleta_casoParcial() {
        SolicitudMovimientoDetalle detalleFallido = crearDetalle("L-1", "Insumo sin stock");
        SolicitudMovimiento solicitud = crearSolicitudConDetalles(List.of(
                crearDetalle("L-2", "Insumo ok 1"),
                crearDetalle("L-3", "Insumo ok 2"),
                detalleFallido
        ));
        solicitud.setId(2L);

        Usuario responsable = new Usuario();
        responsable.setId(20L);

        ArgumentCaptor<SolicitudMovimiento> captor = ArgumentCaptor.forClass(SolicitudMovimiento.class);

        when(repository.findByIdWithLock(2L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(responsable));
        when(repository.saveAndFlush(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        doThrow(new RuntimeException("STOCK_INSUFICIENTE"))
                .when(reservaLoteService).crearOActualizarDesdeDetalle(detalleFallido);

        mockAutenticacion();

        assertThat(solicitud.getDetalles()).hasSize(3);
        var respuesta = service.autorizarSolicitudCompleta(2L, 20L);

        assertThat(respuesta.getEstado()).isEqualTo(EstadoSolicitudMovimiento.PARCIAL);
        assertThat(respuesta.getFechaResolucion()).isNotNull();
        verify(reservaLoteService, times(3)).crearOActualizarDesdeDetalle(any());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoSolicitudMovimiento.PARCIAL);
    }

    private SolicitudMovimiento crearSolicitudConDetalles(List<SolicitudMovimientoDetalle> detalles) {
        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(1L)
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(detalles)
                .build();
        return solicitud;
    }

    private SolicitudMovimientoDetalle crearDetalle(String codigoLote, String nombreProducto) {
        Producto producto = new Producto();
        producto.setNombre(nombreProducto);

        LoteProducto lote = new LoteProducto();
        lote.setId((long) codigoLote.hashCode());
        lote.setCodigoLote(codigoLote);
        lote.setProducto(producto);

        return SolicitudMovimientoDetalle.builder()
                .id(DETALLE_ID.getAndIncrement())
                .lote(lote)
                .cantidad(BigDecimal.ONE)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .build();
    }

    private void mockAutenticacion() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }
}
