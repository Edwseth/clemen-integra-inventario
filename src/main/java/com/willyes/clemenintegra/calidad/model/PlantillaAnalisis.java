package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.TipoAnalisisPlantilla;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plantillas_analisis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaAnalisis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_plantillas_analisis_producto"))
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_analisis", nullable = false, length = 20)
    private TipoAnalisisPlantilla tipoAnalisis;

    @Column(name = "nombre", length = 255)
    private String nombre;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "vigente", nullable = false)
    @Builder.Default
    private boolean vigente = false;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", foreignKey = @ForeignKey(name = "fk_plantillas_analisis_created_by"))
    private Usuario creadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id", foreignKey = @ForeignKey(name = "fk_plantillas_analisis_updated_by"))
    private Usuario actualizadoPor;

    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<PlantillaCampo> campos = new ArrayList<>();

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
