package com.willyes.clemenintegra.calidad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionCalidadDetalleDTO {
    private Long idEvaluacion;
    private Long loteId;
    private String codigoLote;
    private String nombreProducto;
    private String tipoEvaluacion;
    private LocalDateTime fechaEvaluacion;
    private String resultado;
    private String observaciones;

    private boolean requiereAnalisisFisico;
    private boolean requiereAnalisisQuimico;
    private boolean requiereAnalisisQuimicoMicro;
    private boolean requiereAnalisisMicrobiologico;

    private boolean tieneEvaluacionFisica;
    private boolean tieneEvaluacionQuimicaMicro;

    private boolean tieneResultadosFisicos;
    private boolean tieneResultadosQuimicos;
    private boolean tieneResultadosMicro;

    private String estadoFisico;
    private String estadoQuimicoMicrobiologico;
    private String estadoMicrobiologico;

    private List<ResultadoAnalisisMicroDetalleDTO> resultadosMicro;
    private List<ArchivoEvaluacionDTO> archivosAdjuntos;
}
