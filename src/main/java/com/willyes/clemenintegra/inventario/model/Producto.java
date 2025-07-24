package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.*;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos", uniqueConstraints = {
        @UniqueConstraint(name = "un_sku_UNIQUE", columnNames = "codigo_sku"),
        @UniqueConstraint(name = "un_nombre_producto_UNIQUE", columnNames = "nombre")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "codigo_sku", nullable = false, length = 50)
    private String codigoSku;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion_producto", length = 255)
    private String descripcionProducto;

    @Column(name = "stock_actual", nullable = false)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "stock_minimo_proveedor")
    private BigDecimal stockMinimoProveedor;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_analisis", nullable = false, length = 30)
    private TipoAnalisisCalidad tipoAnalisis = TipoAnalisisCalidad.NINGUNO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidades_medida_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_productos_unidades_medida"))
    private UnidadMedida unidadMedida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorias_producto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_productos_categorias_producto1"))
    private CategoriaProducto categoriaProducto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuarios_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_productos_usuarios1"))
    private Usuario creadoPor;

    public Producto(Integer id) {
        this.id = id;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }

    public TipoAnalisisCalidad getTipoAnalisis() {return tipoAnalisis;}
    public void setTipoAnalisis(TipoAnalisisCalidad tipoAnalisis) {this.tipoAnalisis = tipoAnalisis;}

    public Integer getId() {return id;}
    public void setId(Integer id) {this.id = id;}
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
    public boolean isActivo() {return activo;}
    public void setActivo(boolean activo) {this.activo = activo;}
    public LocalDateTime getFechaCreacion() {return fechaCreacion;}
    public void setFechaCreacion(LocalDateTime fechaCreacion) {this.fechaCreacion = fechaCreacion;}
    public UnidadMedida getUnidadMedida() {return unidadMedida;}
    public void setUnidadMedida(UnidadMedida unidadMedida) {this.unidadMedida = unidadMedida;}
    public CategoriaProducto getCategoriaProducto() {return categoriaProducto;}
    public void setCategoriaProducto(CategoriaProducto categoriaProducto) {this.categoriaProducto = categoriaProducto;}
    public Usuario getCreadoPor() {return creadoPor;}
    public void setCreadoPor(Usuario creadoPor) {this.creadoPor = creadoPor;}

}


