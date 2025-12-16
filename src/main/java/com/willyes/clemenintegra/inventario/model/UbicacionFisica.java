package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ubicaciones_fisicas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ubicaciones_fisicas_codigo", columnNames = {"almacen_id", "codigo"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UbicacionFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ubicaciones_fisicas_almacen"))
    private Almacen almacen;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_ubicaciones_fisicas_usuario"))
    private Usuario usuario;

    @PrePersist
    public void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
