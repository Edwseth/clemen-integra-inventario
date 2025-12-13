package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class NoConformidadMapper {

    public NoConformidadDTO toDTO(NoConformidad entity) {
        var lote = entity.getLote();
        var usuario = entity.getUsuarioReporta();
        var producto = entity.getProducto();

        return NoConformidadDTO.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .origen(entity.getOrigen())
                .severidad(entity.getSeveridad())
                .estado(entity.getEstado())
                .tipoIncidente(entity.getTipoIncidente())
                .descripcion(entity.getDescripcion())
                .evidencia(entity.getEvidencia())
                .fechaRegistro(entity.getFechaRegistro())
                .fechaCierre(entity.getFechaCierre())
                .usuarioReportaId(usuario != null ? usuario.getId() : null)
                .loteId(lote != null ? lote.getId() : null)
                .productoId(producto != null && producto.getId() != null ? producto.getId().longValue() : null)
                .evaluacionId(entity.getEvaluacion() != null ? entity.getEvaluacion().getId() : null)
                .codigoLote(lote != null ? lote.getCodigoLote() : null)
                .reportadoPorNombre(usuario != null ? usuario.getNombreCompleto() : null)
                .productoNombre(producto != null ? producto.getNombre() : null)
                .creadoPor(entity.getCreadoPor())
                .actualizadoPor(entity.getActualizadoPor())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }

    public NoConformidad toEntity(NoConformidadDTO dto, Usuario usuarioReporta) {
        LoteProducto lote = dto.getLoteId() != null ? new LoteProducto(dto.getLoteId()) : null;
        Producto producto = dto.getProductoId() != null
                ? Producto.builder().id(dto.getProductoId().intValue()).build()
                : null;
        EvaluacionCalidad evaluacion = dto.getEvaluacionId() != null ? EvaluacionCalidad.builder().id(dto.getEvaluacionId()).build() : null;

        return NoConformidad.builder()
                .id(dto.getId())
                .codigo(dto.getCodigo())
                .origen(dto.getOrigen())
                .severidad(dto.getSeveridad())
                .estado(dto.getEstado())
                .tipoIncidente(dto.getTipoIncidente())
                .descripcion(dto.getDescripcion())
                .evidencia(dto.getEvidencia())
                .fechaRegistro(dto.getFechaRegistro())
                .fechaCierre(dto.getFechaCierre())
                .usuarioReporta(usuarioReporta)
                .lote(lote)
                .producto(producto)
                .evaluacion(evaluacion)
                .creadoPor(dto.getCreadoPor())
                .actualizadoPor(dto.getActualizadoPor())
                .actualizadoEn(dto.getActualizadoEn())
                .build();
    }
}
