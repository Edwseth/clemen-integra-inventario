package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Especificación físico/química por producto para parametrizar límites de aceptación.
 */
@Entity
@Table(name = "especificaciones_fisico_quimicas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecificacionFisicoQuimica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_espec_fq_producto"))
    private Producto producto;

    @Column(name = "nombre_parametro", nullable = false, length = 255)
    private String nombreParametro;

    @Column(name = "unidad", length = 100)
    private String unidad;

    @Column(name = "limite_inferior", precision = 10, scale = 2)
    private BigDecimal limiteInferior;

    @Column(name = "limite_superior", precision = 10, scale = 2)
    private BigDecimal limiteSuperior;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id",
            foreignKey = @ForeignKey(name = "fk_espec_fq_creado_por"))
    private Usuario creadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id",
            foreignKey = @ForeignKey(name = "fk_espec_fq_actualizado_por"))
    private Usuario actualizadoPor;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
