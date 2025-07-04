package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraResponseDTO {
    private Long id;
    private String codigoOrden;
    private String estado;
    private String proveedorNombre;
    private LocalDateTime fechaOrden;

    public String getEstado() {return estado;}
    public String getProveedorNombre() {return proveedorNombre;}
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getCodigoOrden() {return codigoOrden;}
    public void setCodigoOrden(String codigoOrden) {this.codigoOrden = codigoOrden;}
    public void setProveedorNombre(String proveedorNombre) {this.proveedorNombre = proveedorNombre;}
    public void setEstado(String estado) {this.estado = estado;}
    public LocalDateTime getFechaOrden() {return fechaOrden;}
    public void setFechaOrden(LocalDateTime fechaOrden) {this.fechaOrden = fechaOrden;}
}

