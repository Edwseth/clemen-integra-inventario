package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoCondicionUso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCalidadLoteResponseDTO {

    private Long loteId;
    private String codigoLote;
    private String estadoLote;
    private boolean retencionActiva;
    private MotivoRetencion motivoRetencion;
    private NcResumen nc;
    private boolean condicionUsoActiva;
    private CondicionUsoResumen condicionUso;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NcResumen {
        private Long id;
        private SeveridadNoConformidad severidad;
        private EstadoNoConformidad estado;
        private  String reportadoPorNombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CondicionUsoResumen {
        private Long id;
        private TipoCondicionUso tipo;
        private LocalDateTime parametroFecha;
        private String descripcion;
    }
}
