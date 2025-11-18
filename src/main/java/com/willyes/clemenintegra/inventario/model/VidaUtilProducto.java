package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    // 🔹 Relación solo para lectura, NO controla la columna
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "producto_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_vida_util_productos_producto")
    )
    private Producto producto;

    @Column(name = "semanas_vigencia", nullable = false)
    private Integer semanasVigencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por_id")
    private Usuario actualizadoPor;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}


