package com.willyes.clemenintegra.inventario.dto;

import java.time.LocalDateTime;

public class HistorialEstadoOrdenRequest {
    public Long ordenCompraId;
    public String estado;
    public LocalDateTime fechaCambio;
    public Long usuarioId;
    public String observaciones;
}
