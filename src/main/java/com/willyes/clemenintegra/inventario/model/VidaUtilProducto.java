package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vida_util_productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VidaUtilProducto {

    @Id
    @Column(name = "producto_id")
    @EqualsAndHashCode.Include
    private Integer productoId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "producto_id", foreignKey = @ForeignKey(name = "fk_vida_util_productos_producto"))
    private Producto producto;

    @Column(name = "semanas_vigencia", nullable = false)
    private Integer semanasVigencia;
}

