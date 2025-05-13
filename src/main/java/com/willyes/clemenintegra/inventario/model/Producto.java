package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "productos", uniqueConstraints = {
        @UniqueConstraint(name = "un_sku_UNIQUE", columnNames = "codigo_sku"),
        @UniqueConstraint(name = "un_nombre_producto_UNIQUE", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "codigo_sku", nullable = false, length = 50)
    private String codigoSku;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion_producto", length = 255)
    private String descripcionProducto;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 0;

    @Column(name = "stock_minimo_proveedor")
    private Integer stockMinimoProveedor;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "requiere_inspeccion", nullable = false)
    private Boolean requiereInspeccion = false;

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

    public Producto(Long id) {
        this.id = id;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}


