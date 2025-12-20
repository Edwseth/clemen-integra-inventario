package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.CapaRepository;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
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
    @Mock
    private EvaluacionCalidadRepository evaluacionCalidadRepository;
    @Mock
    private ResultadoAnalisisMicrobiologicoRepository resultadoAnalisisMicrobiologicoRepository;

    @InjectMocks
    private AuditoriaLoteServiceImpl service;

    @Test
    void armaAuditoriaConDatosBasicos() {
        Producto producto = new Producto();
        producto.setNombre("Producto");
        producto.setRequiereAnalisisFisico(true);

        LoteProducto lote = LoteProducto.builder()
                .id(1L)
                .codigoLote("LOT-01")
                .estado(EstadoLote.EN_CUARENTENA)
                .stockLote(BigDecimal.TEN)
                .almacen(Almacen.builder().nombre("Principal").ubicacion("A1").build())
                .producto(producto)
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

        EvaluacionCalidad evaluacion = EvaluacionCalidad.builder()
                .id(11L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion.CONFORME)
                .fechaEvaluacion(LocalDateTime.now())
                .usuarioEvaluador(Usuario.builder().nombreCompleto("Ana").build())
                .archivosAdjuntos(List.of(com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion.builder()
                        .nombreArchivo("micro.pdf")
                        .nombreVisible("Microbiológico")
                        .build()))
                .build();

        ResultadoAnalisisMicrobiologico resMicro = ResultadoAnalisisMicrobiologico.builder()
                .id(99L)
                .evaluacion(evaluacion)
                .cumple(true)
                .build();

        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(loteProductoService.obtenerEstadoCalidad(1L)).thenReturn(estado);
        when(noConformidadRepository.findByLote_Id(1L)).thenReturn(List.of(nc));
        when(capaRepository.existsByNoConformidad_Id(5L)).thenReturn(true);
        when(retencionLoteService.obtenerRetencionesActivas(1L)).thenReturn(List.of(retencion));
        when(condicionUsoService.getActivasByLote(1L)).thenReturn(List.of(CondicionUsoResponseDTO.builder().id(7L).descripcion("Condición").build()));
        when(movimientoInventarioRepository.findByLote_IdOrderByFechaIngresoDesc(1L)).thenReturn(List.of(mov));
        when(evaluacionCalidadRepository.findByLoteProductoIdWithAdjuntos(1L)).thenReturn(List.of(evaluacion));
        when(resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(List.of(11L)))
                .thenReturn(List.of(resMicro));

        AuditoriaLoteResponseDTO dto = service.obtenerAuditoriaDeLote(1L);

        assertThat(dto.getCodigoLote()).isEqualTo("LOT-01");
        assertThat(dto.getEstadoCalidad().getEstadoLote()).isEqualTo("EN_CUARENTENA");
        assertThat(dto.getIncidentes()).hasSize(1);
        assertThat(dto.getIncidentes().get(0).isTieneCapa()).isTrue();
        assertThat(dto.getRetenciones()).hasSize(1);
        assertThat(dto.isInconsistenciaRetencion()).isFalse();
        assertThat(dto.getMovimientos()).hasSize(1);
        assertThat(dto.getMovimientos().get(0).getFecha()).isEqualTo(mov.getFechaIngreso());
        assertThat(dto.getEvaluaciones()).hasSize(1);
        assertThat(dto.getEvaluaciones().get(0).isTieneResultadosMicro()).isTrue();
        assertThat(dto.isTieneNoConformidadAsociada()).isTrue();
        assertThat(dto.isTieneNoConformidadActiva()).isTrue();
        assertThat(dto.getCodigoNoConformidadPrincipal()).isEqualTo("NC-001");
        assertThat(dto.getEstadoNoConformidadPrincipal()).isEqualTo("ABIERTA");
    }

    @Test
    void auditoriaSoloFisicoMarcaDisciplinas() {
        Producto producto = new Producto();
        producto.setNombre("Etiqueta");
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);

        LoteProducto lote = LoteProducto.builder()
                .id(2L)
                .codigoLote("L-191225-02")
                .estado(EstadoLote.EN_CUARENTENA)
                .producto(producto)
                .almacen(Almacen.builder().nombre("Cuarentena").build())
                .build();

        EvaluacionCalidad evaluacionFisico = EvaluacionCalidad.builder()
                .id(12L)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .resultado(com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion.CONFORME)
                .fechaEvaluacion(LocalDateTime.now())
                .usuarioEvaluador(Usuario.builder().nombreCompleto("Analista").build())
                .build();

        when(loteProductoRepository.findById(2L)).thenReturn(Optional.of(lote));
        when(loteProductoService.obtenerEstadoCalidad(2L))
                .thenReturn(EstadoCalidadLoteResponseDTO.builder().estadoLote("EN_CUARENTENA").build());
        when(noConformidadRepository.findByLote_Id(2L)).thenReturn(List.of());
        when(retencionLoteService.obtenerRetencionesActivas(2L)).thenReturn(List.of());
        when(condicionUsoService.getActivasByLote(2L)).thenReturn(List.of());
        when(movimientoInventarioRepository.findByLote_IdOrderByFechaIngresoDesc(2L)).thenReturn(List.of());
        when(evaluacionCalidadRepository.findByLoteProductoIdWithAdjuntos(2L)).thenReturn(List.of(evaluacionFisico));

        AuditoriaLoteResponseDTO dto = service.obtenerAuditoriaDeLote(2L);

        assertThat(dto.getDatosLote().getProductoNombre()).isEqualTo("Etiqueta");
        assertThat(dto.getDatosLote().getEstado()).isEqualTo("EN_CUARENTENA");
        assertThat(dto.getDatosLote().getAlmacen()).isEqualTo("Cuarentena");
        assertThat(dto.getCalidad().getFisico().isRequerido()).isTrue();
        assertThat(dto.getCalidad().getFisico().getEstado()).isEqualTo("EVALUADO");
        assertThat(dto.getCalidad().getFisico().getResultado()).isEqualTo("CONFORME");
        assertThat(dto.getCalidad().getQuimicoMicrobiologico().isRequerido()).isFalse();
        assertThat(dto.getCalidad().getQuimicoMicrobiologico().getEstado()).isEqualTo("NO_REQUERIDO");
        assertThat(dto.getCalidad().getMicrobiologico().isRequerido()).isFalse();
        assertThat(dto.getCalidad().getMicrobiologico().getEstado()).isEqualTo("NO_REQUERIDO");
    }

    @Test
    void auditoriaMarcaMicroPendienteCuandoNoHayResultados() {
        Producto producto = new Producto();
        producto.setNombre("Producto QM");
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        LoteProducto lote = LoteProducto.builder()
                .id(3L)
                .codigoLote("L-QM-01")
                .estado(EstadoLote.EN_CUARENTENA)
                .producto(producto)
                .build();

        EvaluacionCalidad evalQM = EvaluacionCalidad.builder()
                .id(13L)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion.CONFORME)
                .fechaEvaluacion(LocalDateTime.now())
                .usuarioEvaluador(Usuario.builder().nombreCompleto("Micro").build())
                .build();

        when(loteProductoRepository.findById(3L)).thenReturn(Optional.of(lote));
        when(loteProductoService.obtenerEstadoCalidad(3L))
                .thenReturn(EstadoCalidadLoteResponseDTO.builder().estadoLote("EN_CUARENTENA").build());
        when(noConformidadRepository.findByLote_Id(3L)).thenReturn(List.of());
        when(retencionLoteService.obtenerRetencionesActivas(3L)).thenReturn(List.of());
        when(condicionUsoService.getActivasByLote(3L)).thenReturn(List.of());
        when(movimientoInventarioRepository.findByLote_IdOrderByFechaIngresoDesc(3L)).thenReturn(List.of());
        when(evaluacionCalidadRepository.findByLoteProductoIdWithAdjuntos(3L)).thenReturn(List.of(evalQM));
        when(resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(List.of(13L)))
                .thenReturn(List.of());

        AuditoriaLoteResponseDTO dto = service.obtenerAuditoriaDeLote(3L);

        assertThat(dto.getCalidad().getQuimicoMicrobiologico().isRequerido()).isTrue();
        assertThat(dto.getCalidad().getQuimicoMicrobiologico().getEstado()).isEqualTo("EVALUADO");
        assertThat(dto.getCalidad().getMicrobiologico().isRequerido()).isTrue();
        assertThat(dto.getCalidad().getMicrobiologico().getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    void auditoriaExponeEstadoRetenidoYRetencionActiva() {
        LoteProducto lote = LoteProducto.builder()
                .id(4L)
                .codigoLote("RET-01")
                .estado(EstadoLote.RETENIDO)
                .build();

        EstadoCalidadLoteResponseDTO estado = EstadoCalidadLoteResponseDTO.builder()
                .estadoLote("RETENIDO")
                .tieneRetencionActiva(false)
                .build();

        when(loteProductoRepository.findById(4L)).thenReturn(Optional.of(lote));
        when(loteProductoService.obtenerEstadoCalidad(4L)).thenReturn(estado);
        NoConformidad nc = NoConformidad.builder()
                .id(8L)
                .codigo("NC-RET-01")
                .estado(EstadoNoConformidad.ABIERTA)
                .fechaRegistro(LocalDateTime.now())
                .build();
        when(noConformidadRepository.findByLote_Id(4L)).thenReturn(List.of(nc));
        when(retencionLoteService.obtenerRetencionesActivas(4L)).thenReturn(List.of());
        when(condicionUsoService.getActivasByLote(4L)).thenReturn(List.of());
        when(movimientoInventarioRepository.findByLote_IdOrderByFechaIngresoDesc(4L)).thenReturn(List.of());
        when(evaluacionCalidadRepository.findByLoteProductoIdWithAdjuntos(4L)).thenReturn(List.of());

        AuditoriaLoteResponseDTO dto = service.obtenerAuditoriaDeLote(4L);

        assertThat(dto.getEstadoLote()).isEqualTo("RETENIDO");
        assertThat(dto.getEstadoCalidad().getEstadoLote()).isEqualTo("RETENIDO");
        assertThat(dto.getEstadoCalidad().isTieneRetencionActiva()).isFalse();
        assertThat(dto.isInconsistenciaRetencion()).isTrue();
        assertThat(dto.getMotivoRetencion()).isEqualTo("NC");
        assertThat(dto.isTieneNoConformidadAsociada()).isTrue();
        assertThat(dto.isTieneNoConformidadActiva()).isTrue();
    }

    @Test
    void lanzaNotFoundCuandoNoExisteLote() {
        when(loteProductoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerAuditoriaDeLote(9L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
