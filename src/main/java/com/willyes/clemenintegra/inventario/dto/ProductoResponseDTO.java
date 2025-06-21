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
        this.id = producto.getId();
        this.codigoSku = producto.getCodigoSku();
        this.nombre = producto.getNombre();
        this.descripcionProducto = producto.getDescripcionProducto();
        this.stockActual = producto.getStockActual();
        this.stockMinimo = producto.getStockMinimo();
        this.stockMinimoProveedor = producto.getStockMinimoProveedor();
        this.activo = producto.isActivo();
        this.requiereInspeccion = producto.isRequiereInspeccion();
        this.unidadMedida = producto.getUnidadMedida() != null ? producto.getUnidadMedida().getNombre() : null;
        this.categoria = producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : null;
        this.fechaCreacion = producto.getFechaCreacion();
    }

}

