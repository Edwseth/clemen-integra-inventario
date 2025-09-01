package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponseDTO {
    private Long id;
    @JsonProperty("sku")
    @JsonAlias("codigoSku")
    private String sku;
    private String nombre;
    private String descripcionProducto;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal stockMinimoProveedor;
    private Boolean activo;
    private String tipoAnalisisCalidad;
    private BigDecimal rendimiento;
    private UnidadMedidaResponseDTO unidadMedida;
    private String categoria;
    private LocalDateTime fechaCreacion;
    // PROD-DETAIL-IDS BEGIN
    private Long unidadMedidaId;
    private Long categoriaProductoId;
    // PROD-DETAIL-IDS END
    // PROD-FLAGS BEGIN
    private Boolean editable;
    private Boolean eliminable;
    private Boolean inactivable;
    // PROD-FLAGS END

    public ProductoResponseDTO(Producto producto) {
        this.id = producto.getId().longValue();
        this.sku = producto.getCodigoSku();
        this.nombre = producto.getNombre();
        this.descripcionProducto = producto.getDescripcionProducto();
        this.stockActual = producto.getStockActual();
        this.stockMinimo = producto.getStockMinimo();
        this.stockMinimoProveedor = producto.getStockMinimoProveedor();
        this.activo = producto.isActivo();
        this.tipoAnalisisCalidad = producto.getTipoAnalisis() != null ? producto.getTipoAnalisis().name() : null;
        this.rendimiento = producto.getRendimientoUnidad() != null ? producto.getRendimientoUnidad() : BigDecimal.ZERO;
        this.unidadMedida = producto.getUnidadMedida() != null
                ? new UnidadMedidaResponseDTO(
                        producto.getUnidadMedida().getId(),
                        producto.getUnidadMedida().getNombre(),
                        producto.getUnidadMedida().getSimbolo())
                : null;
        this.categoria = producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : null;
        this.fechaCreacion = producto.getFechaCreacion();
        // PROD-DETAIL-IDS BEGIN
        this.unidadMedidaId = producto.getUnidadMedida() != null ? producto.getUnidadMedida().getId() : null;
        this.categoriaProductoId = producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getId() : null;
        // PROD-DETAIL-IDS END
        // PROD-FLAGS BEGIN
        this.editable = producto.isActivo();
        this.inactivable = true;
        // PROD-FLAGS END
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getSku() {return sku;}
    public void setSku(String sku) {this.sku = sku;}
    @JsonProperty("codigoSku")
    public String getCodigoSku() {return sku;}
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
    public String getTipoAnalisisCalidad() {return tipoAnalisisCalidad;}
    public void setTipoAnalisisCalidad(String tipoAnalisisCalidad) {this.tipoAnalisisCalidad = tipoAnalisisCalidad;}
    public BigDecimal getRendimiento() {return rendimiento;}
    public void setRendimiento(BigDecimal rendimiento) {this.rendimiento = rendimiento;}
    public UnidadMedidaResponseDTO getUnidadMedida() {return unidadMedida;}
    public void setUnidadMedida(UnidadMedidaResponseDTO unidadMedida) {this.unidadMedida = unidadMedida;}
    public String getCategoria() {return categoria;}
    public void setCategoria(String categoria) {this.categoria = categoria;}
    public LocalDateTime getFechaCreacion() {return fechaCreacion;}
    public void setFechaCreacion(LocalDateTime fechaCreacion) {this.fechaCreacion = fechaCreacion;}
    // PROD-DETAIL-IDS BEGIN
    public Long getUnidadMedidaId() {return unidadMedidaId;}
    public void setUnidadMedidaId(Long unidadMedidaId) {this.unidadMedidaId = unidadMedidaId;}
    public Long getCategoriaProductoId() {return categoriaProductoId;}
    public void setCategoriaProductoId(Long categoriaProductoId) {this.categoriaProductoId = categoriaProductoId;}
    // PROD-DETAIL-IDS END
    // PROD-FLAGS BEGIN
    public Boolean getEditable() {return editable;}
    public void setEditable(Boolean editable) {this.editable = editable;}
    public Boolean getEliminable() {return eliminable;}
    public void setEliminable(Boolean eliminable) {this.eliminable = eliminable;}
    public Boolean getInactivable() {return inactivable;}
    public void setInactivable(Boolean inactivable) {this.inactivable = inactivable;}
    // PROD-FLAGS END
}

