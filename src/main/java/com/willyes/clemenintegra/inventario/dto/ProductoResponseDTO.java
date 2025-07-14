package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.Producto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponseDTO {
    private Long id;
    private String codigoSku;
    private String nombre;
    private String descripcionProducto;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal stockMinimoProveedor;
    private Boolean activo;
    private Boolean requiereInspeccion;
    private String unidadMedida;
    private String categoria;
    private LocalDateTime fechaCreacion;

    public ProductoResponseDTO(Producto producto) {
        this.id = producto.getId().longValue();
        this.codigoSku = producto.getCodigoSku();
        this.nombre = producto.getNombre();
        this.descripcionProducto = producto.getDescripcionProducto();
        this.stockActual = producto.getStockActual();
        this.stockMinimo = producto.getStockMinimo();
        this.stockMinimoProveedor = producto.getStockMinimoProveedor();
        this.activo = producto.isActivo();
        this.requiereInspeccion = producto.getRequiereInspeccion();
        this.unidadMedida = producto.getUnidadMedida() != null ? producto.getUnidadMedida().getNombre() : null;
        this.categoria = producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : null;
        this.fechaCreacion = producto.getFechaCreacion();
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getCodigoSku() {return codigoSku;}
    public void setCodigoSku(String codigoSku) {this.codigoSku = codigoSku;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public String getDescripcionProducto() {return descripcionProducto;}
    public void setDescripcionProducto(String descripcionProducto) {this.descripcionProducto = descripcionProducto;}
    public BigDecimal getStockActual() {return stockActual;}
    public void setStockActual(BigDecimal stockActual) {this.stockActual = stockActual;}
    public BigDecimal getStockMinimo() {return stockMinimo;}
    public void setStockMinimo(BigDecimal stockMinimo) {this.stockMinimo = stockMinimo;}
    public BigDecimal getStockMinimoProveedor() {return stockMinimoProveedor;}
    public void setStockMinimoProveedor(BigDecimal stockMinimoProveedor) {this.stockMinimoProveedor = stockMinimoProveedor;}
    public Boolean getActivo() {return activo;}
    public void setActivo(Boolean activo) {this.activo = activo;}
    public Boolean getRequiereInspeccion() {return requiereInspeccion;}
    public void setRequiereInspeccion(Boolean requiereInspeccion) {this.requiereInspeccion = requiereInspeccion;}
    public String getUnidadMedida() {return unidadMedida;}
    public void setUnidadMedida(String unidadMedida) {this.unidadMedida = unidadMedida;}
    public String getCategoria() {return categoria;}
    public void setCategoria(String categoria) {this.categoria = categoria;}
    public LocalDateTime getFechaCreacion() {return fechaCreacion;}
    public void setFechaCreacion(LocalDateTime fechaCreacion) {this.fechaCreacion = fechaCreacion;}
}

