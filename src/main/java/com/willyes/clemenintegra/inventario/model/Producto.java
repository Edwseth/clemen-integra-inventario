package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.shared.model.*;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
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

    @Column(name = "stock_minimo", nullable = false)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "stock_minimo_proveedor")
    private BigDecimal stockMinimoProveedor;

    @Column(name = "lead_time_compra_dias")
    private Integer leadTimeCompraDias;

    @Column(name = "lead_time_produccion_dias")
    private Integer leadTimeProduccionDias;

    @Column(name = "stock_seguridad", precision = 19, scale = 6)
    private BigDecimal stockSeguridad;

    @Column(name = "stock_maximo_planeacion", precision = 19, scale = 6)
    private BigDecimal stockMaximoPlaneacion;

    /**
     * Rendimiento de producción expresado en la unidad del producto.
     * Representa, por ejemplo, el volumen por unidad de empaque.
     */
    @Column(name = "rendimiento_unidad", precision = 38, scale = 2, nullable = true)
    private BigDecimal rendimientoUnidad;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "tipo_analisis_calidad",
            nullable = false,
            length = 30,
            columnDefinition = "ENUM('NINGUNO','FISICO','QUIMICO_MICROBIOLOGICO','AMBOS')"
    )
    private TipoAnalisisCalidad tipoAnalisis = TipoAnalisisCalidad.NINGUNO;

    // Nuevas banderas por disciplina. Usar estas propiedades para reglas de negocio de calidad.
    @Column(name = "requiere_analisis_fisico", nullable = false)
    private boolean requiereAnalisisFisico;

    @Column(name = "requiere_analisis_quimico", nullable = false)
    private boolean requiereAnalisisQuimico;

    @Column(name = "requiere_analisis_microbiologico", nullable = false)
    private boolean requiereAnalisisMicrobiologico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidades_medida_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_productos_unidades_medida"))
    private UnidadMedida unidadMedida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorias_producto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_productos_categorias_producto1"))
    private CategoriaProducto categoriaProducto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_micro_id",
            foreignKey = @ForeignKey(name = "fk_productos_plantilla_micro"))
    private PlantillaAnalisisMicrobiologico plantillaAnalisisMicrobiologico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuarios_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_productos_usuarios1"))
    private Usuario creadoPor;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "modo_control_inventario", nullable = false, length = 30)
    private ModoControlInventario modoControlInventario = ModoControlInventario.CONTROL_STOCK;

    public Producto(Integer id) {
        this.id = id;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.modoControlInventario == null) {
            this.modoControlInventario = ModoControlInventario.CONTROL_STOCK;
        }
    }

    public TipoAnalisisCalidad getTipoAnalisis() {return tipoAnalisis;}
    public void setTipoAnalisis(TipoAnalisisCalidad tipoAnalisis) {this.tipoAnalisis = tipoAnalisis;}

    // Nuevos alias para mantener compatibilidad con la propiedad renombrada
    public TipoAnalisisCalidad getTipoAnalisisCalidad() {return tipoAnalisis;}
    public void setTipoAnalisisCalidad(TipoAnalisisCalidad tipoAnalisis) {this.tipoAnalisis = tipoAnalisis;}

    public Integer getId() {return id;}
    public void setId(Integer id) {this.id = id;}
    public String getCodigoSku() {return codigoSku;}
    public void setCodigoSku(String codigoSku) {this.codigoSku = codigoSku;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public String getDescripcionProducto() {return descripcionProducto;}
    public void setDescripcionProducto(String descripcionProducto) {this.descripcionProducto = descripcionProducto;}
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
    public PlantillaAnalisisMicrobiologico getPlantillaAnalisisMicrobiologico() {return plantillaAnalisisMicrobiologico;}
    public void setPlantillaAnalisisMicrobiologico(PlantillaAnalisisMicrobiologico plantillaAnalisisMicrobiologico) {this.plantillaAnalisisMicrobiologico = plantillaAnalisisMicrobiologico;}
    public Usuario getCreadoPor() {return creadoPor;}
    public void setCreadoPor(Usuario creadoPor) {this.creadoPor = creadoPor;}
    public BigDecimal getRendimientoUnidad() {return rendimientoUnidad;}
    public void setRendimientoUnidad(BigDecimal rendimientoUnidad) {this.rendimientoUnidad = rendimientoUnidad;}
    public ModoControlInventario getModoControlInventario() {return modoControlInventario;}
    public void setModoControlInventario(ModoControlInventario modoControlInventario) {this.modoControlInventario = modoControlInventario;}

    public Integer getLeadTimeCompraDias() {return leadTimeCompraDias;}
    public void setLeadTimeCompraDias(Integer leadTimeCompraDias) {this.leadTimeCompraDias = leadTimeCompraDias;}
    public Integer getLeadTimeProduccionDias() {return leadTimeProduccionDias;}
    public void setLeadTimeProduccionDias(Integer leadTimeProduccionDias) {this.leadTimeProduccionDias = leadTimeProduccionDias;}
    public BigDecimal getStockSeguridad() {return stockSeguridad;}
    public void setStockSeguridad(BigDecimal stockSeguridad) {this.stockSeguridad = stockSeguridad;}
    public BigDecimal getStockMaximoPlaneacion() {return stockMaximoPlaneacion;}
    public void setStockMaximoPlaneacion(BigDecimal stockMaximoPlaneacion) {this.stockMaximoPlaneacion = stockMaximoPlaneacion;}

    public boolean isRequiereAnalisisFisico() {return requiereAnalisisFisico;}
    public void setRequiereAnalisisFisico(boolean requiereAnalisisFisico) {this.requiereAnalisisFisico = requiereAnalisisFisico;}
    public boolean isRequiereAnalisisQuimico() {return requiereAnalisisQuimico;}
    public void setRequiereAnalisisQuimico(boolean requiereAnalisisQuimico) {this.requiereAnalisisQuimico = requiereAnalisisQuimico;}
    public boolean isRequiereAnalisisMicrobiologico() {return requiereAnalisisMicrobiologico;}
    public void setRequiereAnalisisMicrobiologico(boolean requiereAnalisisMicrobiologico) {this.requiereAnalisisMicrobiologico = requiereAnalisisMicrobiologico;}

}


