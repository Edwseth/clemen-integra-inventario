package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.service.ArchivoEvaluacionConstants;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.CapaRepository;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditoriaLoteServiceImpl implements AuditoriaLoteService {

    private final LoteProductoRepository loteProductoRepository;
    private final LoteProductoService loteProductoService;
    private final NoConformidadRepository noConformidadRepository;
    private final CapaRepository capaRepository;
    private final RetencionLoteService retencionLoteService;
    private final CondicionUsoService condicionUsoService;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final EvaluacionCalidadRepository evaluacionCalidadRepository;
    private final ResultadoAnalisisMicrobiologicoRepository resultadoAnalisisMicrobiologicoRepository;

    @Override
    @Transactional(readOnly = true)
    public AuditoriaLoteResponseDTO obtenerAuditoriaDeLote(Long loteId) {
        LoteProducto lote = loteProductoRepository.findById(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LOTE_NO_ENCONTRADO"));

        boolean requiereFisico = lote.getProducto() != null && lote.getProducto().isRequiereAnalisisFisico();
        boolean requiereQuimico = lote.getProducto() != null && lote.getProducto().isRequiereAnalisisQuimico();
        boolean requiereMicro = lote.getProducto() != null && lote.getProducto().isRequiereAnalisisMicrobiologico();
        TipoAnalisisCalidad tipoAnalisis = TipoAnalisisCalidad.fromFlags(requiereFisico, requiereQuimico, requiereMicro);

        EstadoCalidadLoteResponseDTO estadoCalidad = loteProductoService.obtenerEstadoCalidad(loteId);

        List<NoConformidad> noConformidades = noConformidadRepository.findByLote_Id(loteId);
        List<NoConformidad> noConformidadesOrdenadas = noConformidades.stream()
                .sorted(Comparator.comparing(NoConformidad::getFechaRegistro,
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo)))
                .toList();
        List<AuditoriaLoteResponseDTO.IncidenteDTO> incidentes = noConformidadesOrdenadas.stream()
                .map(nc -> AuditoriaLoteResponseDTO.IncidenteDTO.builder()
                        .id(nc.getId())
                        .codigo(nc.getCodigo())
                        .tipoIncidente(nc.getTipoIncidente())
                        .severidad(nc.getSeveridad())
                        .estado(nc.getEstado())
                        .fechaApertura(nc.getFechaRegistro())
                        .fechaCierre(nc.getFechaCierre())
                        .tieneCapa(capaRepository.existsByNoConformidad_Id(nc.getId()))
                        .build())
                .toList();

        List<AuditoriaLoteResponseDTO.RetencionDTO> retenciones = retencionLoteService.obtenerRetencionesActivas(loteId).stream()
                .map(this::mapRetencion)
                .toList();

        CondicionUsoResponseDTO condicionUso = condicionUsoService.getActivasByLote(loteId).stream()
                .findFirst()
                .orElse(null);

        List<AuditoriaLoteResponseDTO.MovimientoDTO> movimientos = movimientoInventarioRepository
                .findByLote_IdOrderByFechaIngresoDesc(loteId)
                .stream()
                .map(this::mapMovimiento)
                .toList();

        List<com.willyes.clemenintegra.calidad.model.EvaluacionCalidad> evaluacionesLote =
                evaluacionCalidadRepository.findByLoteProductoIdWithAdjuntos(loteId);
        List<AuditoriaLoteResponseDTO.EvaluacionResumenDTO> evaluaciones = construirResumenEvaluaciones(evaluacionesLote);

        java.util.Set<Long> evaluacionesConResultadosMicro = evaluacionesLote == null
                ? java.util.Set.of()
                : resultadosMicroPorEvaluacion(evaluacionesLote).keySet();

        AnalisisCalidadHelper.EstadoDisciplinasCalidad estadoDisciplinas = AnalisisCalidadHelper.calcularEstadoDisciplinas(
                lote.getProducto(),
                evaluacionesLote,
                evaluacionesConResultadosMicro::contains);

        boolean tieneNoConformidadAsociada = !noConformidadesOrdenadas.isEmpty();
        boolean tieneNoConformidadActiva = noConformidadesOrdenadas.stream()
                .anyMatch(nc -> nc.getEstado() == com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad.ABIERTA);
        Optional<NoConformidad> noConformidadPrincipal = noConformidadesOrdenadas.stream()
                .filter(nc -> nc.getEstado() == com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad.ABIERTA)
                .max(Comparator.comparing(NoConformidad::getFechaRegistro,
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo)));
        if (noConformidadPrincipal.isEmpty()) {
            noConformidadPrincipal = noConformidadesOrdenadas.stream()
                    .max(Comparator.comparing(NoConformidad::getFechaRegistro,
                            Comparator.nullsLast(java.time.LocalDateTime::compareTo)));
        }

        String motivoRetencion = null;
        if (retenciones.isEmpty()
                && lote.getEstado() == com.willyes.clemenintegra.inventario.model.enums.EstadoLote.RETENIDO
                && tieneNoConformidadAsociada) {
            motivoRetencion = "NC";
        }

        String almacenActual = lote.getAlmacen() != null ? lote.getAlmacen().getNombre() : null;
        String ubicacionActual = lote.getAlmacen() != null ? lote.getAlmacen().getUbicacion() : null;
        String almacenDetalle = almacenActual;
        if (almacenDetalle != null && ubicacionActual != null && !ubicacionActual.isBlank()) {
            almacenDetalle = almacenDetalle + " (" + ubicacionActual + ")";
        }

        AuditoriaLoteResponseDTO.DatosLoteDTO datosLote = AuditoriaLoteResponseDTO.DatosLoteDTO.builder()
                .codigoLote(lote.getCodigoLote())
                .productoNombre(lote.getProducto() != null ? lote.getProducto().getNombre() : null)
                .estado(lote.getEstado() != null ? lote.getEstado().name() : null)
                .fechaIngreso(lote.getFechaFabricacion())
                .fechaVencimiento(lote.getFechaVencimiento())
                .almacen(almacenDetalle)
                .tipoAnalisisRequerido(tipoAnalisis != null ? tipoAnalisis.name() : null)
                .build();

        AuditoriaLoteResponseDTO.CalidadLoteAuditoriaDTO calidad = AuditoriaLoteResponseDTO.CalidadLoteAuditoriaDTO.builder()
                .tipoAnalisisRequerido(tipoAnalisis != null ? tipoAnalisis.name() : null)
                .fisico(mapDisciplina(estadoDisciplinas.fisico()))
                .quimicoMicrobiologico(mapDisciplina(estadoDisciplinas.quimicoMicrobiologico()))
                .microbiologico(mapDisciplina(estadoDisciplinas.microbiologico()))
                .build();

        return AuditoriaLoteResponseDTO.builder()
                .loteId(lote.getId())
                .codigoLote(lote.getCodigoLote())
                .nombreProducto(lote.getProducto() != null ? lote.getProducto().getNombre() : null)
                .categoriaProducto(lote.getProducto() != null && lote.getProducto().getCategoriaProducto() != null
                        ? lote.getProducto().getCategoriaProducto().getNombre() : null)
                .tipoAnalisisCalidad(tipoAnalisis != null ? tipoAnalisis.name() : null)
                .estadoLote(lote.getEstado() != null ? lote.getEstado().name() : null)
                .fechaFabricacion(lote.getFechaFabricacion())
                .fechaVencimiento(lote.getFechaVencimiento())
                .stockLote(lote.getStockLote())
                .nombreAlmacenActual(almacenActual)
                .ubicacionAlmacenActual(ubicacionActual)
                .datosLote(datosLote)
                .calidad(calidad)
                .estadoCalidad(estadoCalidad)
                .evaluaciones(evaluaciones)
                .incidentes(incidentes)
                .retenciones(retenciones)
                .tieneNoConformidadAsociada(tieneNoConformidadAsociada)
                .tieneNoConformidadActiva(tieneNoConformidadActiva)
                .codigoNoConformidadPrincipal(noConformidadPrincipal.map(NoConformidad::getCodigo).orElse(null))
                .estadoNoConformidadPrincipal(noConformidadPrincipal
                        .map(NoConformidad::getEstado)
                        .map(Enum::name)
                        .orElse(null))
                .motivoRetencion(motivoRetencion)
                .condicionUsoActiva(mapCondicion(condicionUso))
                .movimientos(movimientos)
                .build();
    }

    private AuditoriaLoteResponseDTO.RetencionDTO mapRetencion(RetencionLote retencion) {
        return AuditoriaLoteResponseDTO.RetencionDTO.builder()
                .id(retencion.getId())
                .motivo(retencion.getMotivo())
                .descripcion(retencion.getCausa())
                .estado(retencion.getEstado() != null ? retencion.getEstado().name() : null)
                .build();
    }

    private AuditoriaLoteResponseDTO.CondicionUsoDTO mapCondicion(CondicionUsoResponseDTO condicionUso) {
        if (condicionUso == null) {
            return null;
        }
        return AuditoriaLoteResponseDTO.CondicionUsoDTO.builder()
                .id(condicionUso.getId())
                .tipo(condicionUso.getTipo())
                .parametroFecha(condicionUso.getParametroFecha())
                .descripcion(condicionUso.getDescripcion())
                .build();
    }

    private AuditoriaLoteResponseDTO.MovimientoDTO mapMovimiento(MovimientoInventario mov) {
        String almacenOrigen = mov.getAlmacenOrigen() != null ? mov.getAlmacenOrigen().getNombre() : null;
        String almacenDestino = mov.getAlmacenDestino() != null ? mov.getAlmacenDestino().getNombre() : null;
        return AuditoriaLoteResponseDTO.MovimientoDTO.builder()
                .id(mov.getId())
                .fechaMovimiento(mov.getFechaIngreso())
                .fecha(mov.getFechaIngreso())
                .tipoMovimiento(mov.getTipoMovimiento())
                .clasificacion(mov.getClasificacion())
                .cantidad(mov.getCantidad())
                .almacenOrigenNombre(almacenOrigen)
                .almacenDestinoNombre(almacenDestino)
                .almacenOrigen(almacenOrigen)
                .almacenDestino(almacenDestino)
                .motivoMovimientoNombre(mov.getMotivoMovimiento() != null ? mov.getMotivoMovimiento().getDescripcion() : null)
                .registradoPorNombre(mov.getRegistradoPor() != null ? mov.getRegistradoPor().getNombreCompleto() : null)
                .ordenProduccionCodigo(mov.getOrdenProduccion() != null ? mov.getOrdenProduccion().getCodigoOrden() : null)
                .build();
    }

    private List<AuditoriaLoteResponseDTO.EvaluacionResumenDTO> construirResumenEvaluaciones(
            List<com.willyes.clemenintegra.calidad.model.EvaluacionCalidad> evaluaciones) {
        if (evaluaciones == null || evaluaciones.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ResultadoAnalisisMicrobiologico>> resultadosPorEvaluacion = resultadosMicroPorEvaluacion(evaluaciones);

        return evaluaciones.stream()
                .map(evaluacion -> {
                    List<ResultadoAnalisisMicrobiologico> resultados = resultadosPorEvaluacion.getOrDefault(
                            evaluacion.getId(), List.of());
                    boolean tieneResultadosMicro = !resultados.isEmpty();
                    Boolean conformeMicro = null;
                    if (tieneResultadosMicro) {
                        boolean anyFalse = resultados.stream().anyMatch(r -> Boolean.FALSE.equals(r.getCumple()));
                        conformeMicro = !anyFalse;
                    }

                    List<AuditoriaLoteResponseDTO.EvaluacionAdjuntoDTO> adjuntos = Optional
                            .ofNullable(evaluacion.getArchivosAdjuntos())
                            .orElseGet(List::of)
                            .stream()
                            .map(a -> AuditoriaLoteResponseDTO.EvaluacionAdjuntoDTO.builder()
                                    .nombreArchivo(a.getNombreArchivo())
                                    .nombreVisible(a.getNombreVisible())
                                    .build())
                            .toList();

                    boolean pdfMicroDisponible = adjuntos.stream()
                            .map(AuditoriaLoteResponseDTO.EvaluacionAdjuntoDTO::getNombreVisible)
                            .filter(java.util.Objects::nonNull)
                            .anyMatch(nombre -> nombre.equalsIgnoreCase(ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO));

                    return AuditoriaLoteResponseDTO.EvaluacionResumenDTO.builder()
                            .id(evaluacion.getId())
                            .tipoEvaluacion(evaluacion.getTipoEvaluacion())
                            .resultado(evaluacion.getResultado() != null ? evaluacion.getResultado().name() : null)
                            .fechaEvaluacion(evaluacion.getFechaEvaluacion())
                            .usuarioEvaluador(evaluacion.getUsuarioEvaluador() != null
                                    ? evaluacion.getUsuarioEvaluador().getNombreCompleto() : null)
                            .tieneAdjuntos(!adjuntos.isEmpty())
                            .tieneResultadosMicro(tieneResultadosMicro)
                            .conformeMicro(conformeMicro)
                            .pdfMicroDisponible(pdfMicroDisponible)
                            .adjuntos(adjuntos)
                            .build();
                })
                .toList();
    }

    private Map<Long, List<ResultadoAnalisisMicrobiologico>> resultadosMicroPorEvaluacion(
            List<com.willyes.clemenintegra.calidad.model.EvaluacionCalidad> evaluaciones) {
        List<Long> microIds = evaluaciones.stream()
                .filter(e -> e.getTipoEvaluacion() == com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .map(com.willyes.clemenintegra.calidad.model.EvaluacionCalidad::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, List<ResultadoAnalisisMicrobiologico>> resultadosPorEvaluacion = new java.util.HashMap<>();
        if (!microIds.isEmpty()) {
            for (ResultadoAnalisisMicrobiologico resultado : resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(microIds)) {
                Long evalId = resultado.getEvaluacion() != null ? resultado.getEvaluacion().getId() : null;
                if (evalId != null) {
                    resultadosPorEvaluacion.computeIfAbsent(evalId, key -> new java.util.ArrayList<>()).add(resultado);
                }
            }
        }
        return resultadosPorEvaluacion;
    }

    private AuditoriaLoteResponseDTO.DisciplinaCalidadDTO mapDisciplina(
            AnalisisCalidadHelper.DisciplinaCalidadEstado estado) {
        if (estado == null) {
            return null;
        }
        return AuditoriaLoteResponseDTO.DisciplinaCalidadDTO.builder()
                .requerido(estado.requerido())
                .estado(estado.estado() != null ? estado.estado().name() : null)
                .resultado(estado.resultado())
                .fechaUltimaEvaluacion(estado.fechaUltimaEvaluacion())
                .evaluador(estado.evaluador())
                .build();
    }
}
