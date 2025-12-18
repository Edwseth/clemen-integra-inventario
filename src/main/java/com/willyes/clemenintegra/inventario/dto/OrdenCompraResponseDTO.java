package com.willyes.clemenintegra.inventario.dto;

import lombok.*;

import java.math.BigDecimal;
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
    private LocalDate fechaCompromisoEntrega;
    private BigDecimal descuento;
    private BigDecimal totalPedido;
    private BigDecimal totalRecibido;
    private BigDecimal totalPendiente;
    private BigDecimal porcentajeAvance;

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
    public BigDecimal getDescuento() {return descuento;}
    public void setDescuento(BigDecimal descuento) {this.descuento = descuento;}
}
