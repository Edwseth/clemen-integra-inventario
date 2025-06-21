package com.willyes.clemenintegra.inventario.dto;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraResponseDTO {
    private Long id;
    private String codigoOrden;
    private String estado;
    private String proveedorNombre;

    public String getEstado() {return estado;}
    public String getProveedorNombre() {return proveedorNombre;}

    public void setProveedorNombre(String proveedorNombre) {this.proveedorNombre = proveedorNombre;}
    public void setEstado(String estado) {this.estado = estado;}
}

