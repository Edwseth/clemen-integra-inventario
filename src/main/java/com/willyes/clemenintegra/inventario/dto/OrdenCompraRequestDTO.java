package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraRequestDTO {

    @NotNull
    public String codigoOrden;

    @NotNull
    private Long proveedorId;

    private String observaciones;

    @NotNull
    private List<OrdenCompraDetalleRequestDTO> detalles;

    public String getCodigoOrden() {return codigoOrden;}
}
