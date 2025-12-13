package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.repository.CapaRepository;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
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

    @Override
    @Transactional(readOnly = true)
    public AuditoriaLoteResponseDTO obtenerAuditoriaDeLote(Long loteId) {
        LoteProducto lote = loteProductoRepository.findById(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LOTE_NO_ENCONTRADO"));

        EstadoCalidadLoteResponseDTO estadoCalidad = loteProductoService.obtenerEstadoCalidad(loteId);

        List<AuditoriaLoteResponseDTO.IncidenteDTO> incidentes = noConformidadRepository.findByLote_Id(loteId).stream()
                .sorted(Comparator.comparing(NoConformidad::getFechaRegistro, Comparator.nullsLast(java.time.LocalDateTime::compareTo)))
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

        List<AuditoriaLoteResponseDTO.MovimientoDTO> movimientos = movimientoInventarioRepository.findByLote_IdOrderByFechaIngresoDesc(loteId)
                .stream()
                .map(this::mapMovimiento)
                .toList();

        return AuditoriaLoteResponseDTO.builder()
                .loteId(lote.getId())
                .codigoLote(lote.getCodigoLote())
                .nombreProducto(lote.getProducto() != null ? lote.getProducto().getNombre() : null)
                .categoriaProducto(lote.getProducto() != null && lote.getProducto().getCategoriaProducto() != null
                        ? lote.getProducto().getCategoriaProducto().getNombre() : null)
                .tipoAnalisisCalidad(lote.getProducto() != null && lote.getProducto().getTipoAnalisisCalidad() != null
                        ? lote.getProducto().getTipoAnalisisCalidad().name() : null)
                .estadoLote(lote.getEstado() != null ? lote.getEstado().name() : null)
                .fechaFabricacion(lote.getFechaFabricacion())
                .fechaVencimiento(lote.getFechaVencimiento())
                .stockLote(lote.getStockLote())
                .nombreAlmacenActual(lote.getAlmacen() != null ? lote.getAlmacen().getNombre() : null)
                .ubicacionAlmacenActual(lote.getAlmacen() != null ? lote.getAlmacen().getUbicacion() : null)
                .estadoCalidad(estadoCalidad)
                .incidentes(incidentes)
                .retenciones(retenciones)
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
        return AuditoriaLoteResponseDTO.MovimientoDTO.builder()
                .id(mov.getId())
                .fechaMovimiento(mov.getFechaIngreso())
                .tipoMovimiento(mov.getTipoMovimiento())
                .clasificacion(mov.getClasificacion())
                .cantidad(mov.getCantidad())
                .almacenOrigenNombre(mov.getAlmacenOrigen() != null ? mov.getAlmacenOrigen().getNombre() : null)
                .almacenDestinoNombre(mov.getAlmacenDestino() != null ? mov.getAlmacenDestino().getNombre() : null)
                .motivoMovimientoNombre(mov.getMotivoMovimiento() != null ? mov.getMotivoMovimiento().getDescripcion() : null)
                .registradoPorNombre(mov.getRegistradoPor() != null ? mov.getRegistradoPor().getNombreCompleto() : null)
                .ordenProduccionCodigo(mov.getOrdenProduccion() != null ? mov.getOrdenProduccion().getCodigoOrden() : null)
                .build();
    }
}

