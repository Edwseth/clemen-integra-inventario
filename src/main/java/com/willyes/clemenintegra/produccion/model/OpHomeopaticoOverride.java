package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "op_homeopatico_override")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpHomeopaticoOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_produccion_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_op_homeopatico_override_orden"))
    private OrdenProduccion ordenProduccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_op_homeopatico_override_producto"))
    private Producto producto;

    @Column(name = "semanas_vigencia", nullable = false)
    private Integer semanasVigencia;

    @Column(name = "cantidad_solicitada", nullable = false, precision = 18, scale = 3)
    private BigDecimal cantidadSolicitada;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id",
            foreignKey = @ForeignKey(name = "fk_op_homeopatico_override_usuario"))
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    public void prePersist() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
