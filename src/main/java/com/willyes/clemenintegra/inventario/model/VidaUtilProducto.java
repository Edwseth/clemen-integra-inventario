package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "vida_util_productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VidaUtilProducto implements Persistable<Integer> {

    @Id
    @Column(name = "producto_id", nullable = false)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.NONE)
    private Integer productoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "producto_id",
            referencedColumnName = "id",
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

    @Transient
    private boolean isNew = true;

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public void setProductoId(Integer productoId) {
        this.productoId = productoId;
    }

    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }

    @Override
    public Integer getId() {
        return productoId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}


