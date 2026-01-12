package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoConformidadDetalleDTO {

    private Long id;

    private String codigo;

    private OrigenNoConformidad origen;

    private SeveridadNoConformidad severidad;

    private TipoIncidente tipoIncidente;

    private EstadoNoConformidad estado;

    private String descripcion;

    private String evidencia;

    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaCierre;

    private Long usuarioReportaId;

    private Long loteId;

    private Long productoId;

    private Long evaluacionId;

    private String codigoLote;

    private String reportadoPorNombre;

    private String productoNombre;

    private Long creadoPor;

    private Long actualizadoPor;

    private LocalDateTime actualizadoEn;

    private Boolean retener;
}
