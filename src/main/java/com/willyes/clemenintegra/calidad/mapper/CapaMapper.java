package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.CapaArchivoDTO;
import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.model.Capa;
import com.willyes.clemenintegra.calidad.model.CapaArchivo;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class CapaMapper {

    public CapaDTO toDTO(Capa entity) {
        return toDTO(entity, Collections.emptyList());
    }

    public CapaDTO toDTO(Capa entity, List<CapaArchivo> archivos) {
        return CapaDTO.builder()
                .id(entity.getId())
                .noConformidadId(entity.getNoConformidad().getId())
                .tipo(entity.getTipo())
                .responsableId(entity.getResponsable().getId())
                .fechaInicio(entity.getFechaInicio())
                .fechaCierre(entity.getFechaCierre())
                .estado(entity.getEstado())
                .observaciones(entity.getObservaciones())
                .archivosAdjuntos(Optional.ofNullable(archivos).orElse(List.of()).stream()
                        .map(this::toArchivoDTO)
                        .toList())
                .build();
    }

    public Capa toEntity(CapaDTO dto,
                         com.willyes.clemenintegra.calidad.model.NoConformidad noConformidad,
                         Usuario responsable) {
        LocalDateTime inicio = dto.getFechaInicio() != null ? dto.getFechaInicio() : LocalDateTime.now();
        EstadoCapa estado = dto.getEstado() != null ? dto.getEstado() : EstadoCapa.ACTIVA;
        LocalDateTime cierre = dto.getFechaCierre();
        if (EstadoCapa.CERRADA.equals(estado) && cierre == null) {
            cierre = LocalDateTime.now();
        }

        return Capa.builder()
                .id(dto.getId())
                .noConformidad(noConformidad)
                .tipo(dto.getTipo())
                .responsable(responsable)
                .fechaInicio(inicio)
                .fechaCierre(cierre)
                .estado(estado)
                .observaciones(dto.getObservaciones())
                .build();
    }

    public CapaArchivoDTO toArchivoDTO(CapaArchivo archivo) {
        return CapaArchivoDTO.builder()
                .id(archivo.getId())
                .nombreArchivo(archivo.getNombreArchivo())
                .nombreVisible(archivo.getNombreVisible())
                .contentType(archivo.getContentType())
                .tamanoBytes(archivo.getTamanoBytes())
                .fechaCreacion(archivo.getFechaCreacion())
                .build();
    }
}
