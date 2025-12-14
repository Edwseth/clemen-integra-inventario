package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.produccion.dto.AlistamientoOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduccionAlistamientoServiceImplTest {

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;

    @InjectMocks
    private ProduccionAlistamientoServiceImpl service;

    @Test
    void obtenerAlistamiento_sinSolicitudesDevuelveSinSolicitud() {
        OrdenProduccion orden = crearOrden();
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(solicitudMovimientoRepository.findWithDetalles(eq(orden.getId()), any(), any(), any(), eq(false), any()))
                .thenReturn(List.of());

        AlistamientoOrdenProduccionDTO dto = service.obtenerAlistamientoPorOrden(orden.getId());

        assertThat(dto.getEstadoAlistamiento()).isEqualTo("SIN_SOLICITUD");
        assertThat(dto.getSolicitudes()).isEmpty();
    }

    @Test
    void obtenerAlistamiento_enProcesoCuandoHayPendientes() {
        OrdenProduccion orden = crearOrden();
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(solicitudMovimientoRepository.findWithDetalles(eq(orden.getId()), any(), any(), any(), eq(false), any()))
                .thenReturn(List.of(
                        crearSolicitud(1L, EstadoSolicitudMovimiento.PENDIENTE),
                        crearSolicitud(2L, EstadoSolicitudMovimiento.AUTORIZADA)
                ));

        AlistamientoOrdenProduccionDTO dto = service.obtenerAlistamientoPorOrden(orden.getId());

        assertThat(dto.getEstadoAlistamiento()).isEqualTo("EN_PROCESO");
        assertThat(dto.getSolicitudes()).hasSize(2);
    }

    @Test
    void obtenerAlistamiento_listoCuandoTodasAtendidasOCerradas() {
        OrdenProduccion orden = crearOrden();
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(solicitudMovimientoRepository.findWithDetalles(eq(orden.getId()), any(), any(), any(), eq(false), any()))
                .thenReturn(List.of(
                        crearSolicitud(10L, EstadoSolicitudMovimiento.ATENDIDA),
                        crearSolicitud(11L, EstadoSolicitudMovimiento.CERRADA)
                ));

        AlistamientoOrdenProduccionDTO dto = service.obtenerAlistamientoPorOrden(orden.getId());

        assertThat(dto.getEstadoAlistamiento()).isEqualTo("LISTO");
    }

    @Test
    void obtenerAlistamiento_lanza404CuandoNoExisteOrden() {
        when(ordenProduccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerAlistamientoPorOrden(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Orden de producción no encontrada");
    }

    private OrdenProduccion crearOrden() {
        return OrdenProduccion.builder()
                .id(5L)
                .codigoOrden("OP-01")
                .estado(EstadoProduccion.EN_PROCESO)
                .fechaInicio(LocalDateTime.now())
                .cantidadProgramada(java.math.BigDecimal.ONE)
                .cantidadProducida(java.math.BigDecimal.ZERO)
                .cantidadProducidaAcumulada(java.math.BigDecimal.ZERO)
                .build();
    }

    private SolicitudMovimiento crearSolicitud(Long id, EstadoSolicitudMovimiento estado) {
        Almacen almacenOrigen = new Almacen();
        almacenOrigen.setNombre("Origen");
        Almacen almacenDestino = new Almacen();
        almacenDestino.setNombre("Destino");

        return SolicitudMovimiento.builder()
                .id(id)
                .estado(estado)
                .fechaSolicitud(LocalDateTime.now())
                .almacenOrigen(almacenOrigen)
                .almacenDestino(almacenDestino)
                .build();
    }
}
