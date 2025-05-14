package com.willyes.clemenintegra.produccion.dto;

import java.time.LocalDateTime;

public class DetalleEtapaRequest {
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public String observaciones;
    public Long etapaProduccionId;
    public Long ordenProduccionId;
    public Long operarioId;
}