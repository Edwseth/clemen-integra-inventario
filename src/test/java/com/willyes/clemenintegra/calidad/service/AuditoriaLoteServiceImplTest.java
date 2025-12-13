package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;
import com.willyes.clemenintegra.calidad.repository.CapaRepository;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaLoteServiceImplTest {

    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private LoteProductoService loteProductoService;
    @Mock
    private NoConformidadRepository noConformidadRepository;
    @Mock
    private CapaRepository capaRepository;
    @Mock
    private RetencionLoteService retencionLoteService;
    @Mock
    private CondicionUsoService condicionUsoService;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @InjectMocks
    private AuditoriaLoteServiceImpl service;

    @Test
    void armaAuditoriaConDatosBasicos() {
        LoteProducto lote = LoteProducto.builder()
                .id(1L)
                .codigoLote("LOT-01")
                .estado(EstadoLote.EN_CUARENTENA)
                .stockLote(BigDecimal.TEN)
                .almacen(Almacen.builder().nombre("Principal").ubicacion("A1").build())
                .build();

        EstadoCalidadLoteResponseDTO estado = EstadoCalidadLoteResponseDTO.builder()
                .loteId(1L)
                .estadoLote("EN_CUARENTENA")
                .build();

        NoConformidad nc = NoConformidad.builder()
                .id(5L)
                .codigo("NC-001")
                .tipoIncidente(TipoIncidente.NO_CONFORMIDAD)
                .severidad(SeveridadNoConformidad.MAYOR)
                .estado(EstadoNoConformidad.ABIERTA)
                .fechaRegistro(LocalDateTime.now())
                .build();

        RetencionLote retencion = RetencionLote.builder()
                .id(2L)
                .motivo(MotivoRetencion.NO_CONFORMIDAD)
                .causa("Retención de prueba")
                .estado(EstadoRetencion.RETENIDO)
                .build();

        MovimientoInventario mov = MovimientoInventario.builder()
                .id(3L)
                .fechaIngreso(LocalDateTime.now())
                .tipoMovimiento(TipoMovimiento.ENTRADA)
                .clasificacion(ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO)
                .cantidad(BigDecimal.ONE)
                .motivoMovimiento(com.willyes.clemenintegra.inventario.model.MotivoMovimiento.builder()
                        .descripcion("Ingreso")
                        .build())
                .registradoPor(Usuario.builder().nombreCompleto("Analista").build())
                .build();

        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(loteProductoService.obtenerEstadoCalidad(1L)).thenReturn(estado);
        when(noConformidadRepository.findByLote_Id(1L)).thenReturn(List.of(nc));
        when(capaRepository.existsByNoConformidad_Id(5L)).thenReturn(true);
        when(retencionLoteService.obtenerRetencionesActivas(1L)).thenReturn(List.of(retencion));
        when(condicionUsoService.getActivasByLote(1L)).thenReturn(List.of(CondicionUsoResponseDTO.builder().id(7L).descripcion("Condición").build()));
        when(movimientoInventarioRepository.findByLote_IdOrderByFechaIngresoDesc(1L)).thenReturn(List.of(mov));

        AuditoriaLoteResponseDTO dto = service.obtenerAuditoriaDeLote(1L);

        assertThat(dto.getCodigoLote()).isEqualTo("LOT-01");
        assertThat(dto.getEstadoCalidad().getEstadoLote()).isEqualTo("EN_CUARENTENA");
        assertThat(dto.getIncidentes()).hasSize(1);
        assertThat(dto.getIncidentes().get(0).isTieneCapa()).isTrue();
        assertThat(dto.getRetenciones()).hasSize(1);
        assertThat(dto.getMovimientos()).hasSize(1);
    }

    @Test
    void lanzaNotFoundCuandoNoExisteLote() {
        when(loteProductoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerAuditoriaDeLote(9L))
                .isInstanceOf(ResponseStatusException.class);
    }
}

