package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AlertaOrdenProduccionDTO {

    private Long ordenId;
    private String codigoOrden;
    private String productoPrincipal;
    private LocalDate fechaCompromiso;
    private String estado;
    private String tipoAlerta;
}
