package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;

public class CambioEstadoOrdenRequest {
    public EstadoOrdenCompra estado;
    public Long usuarioId;
    public String observaciones;
}

