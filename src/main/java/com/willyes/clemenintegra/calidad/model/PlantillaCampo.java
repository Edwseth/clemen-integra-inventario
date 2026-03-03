package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.TipoCampoPlantilla;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "plantilla_campos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_plantilla_campos_plantilla"))
    private PlantillaAnalisis plantilla;

    @Column(name = "codigo", nullable = false, length = 80)
    private String codigo;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "tipo_campo",
            nullable = false,
            columnDefinition = "ENUM('triestado','numerico','texto','fecha','select')"
    )
    private TipoCampoPlantilla tipoCampo;

    @Column(name = "requerido", nullable = false)
    @Builder.Default
    private boolean requerido = true;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", foreignKey = @ForeignKey(name = "fk_plantilla_campos_created_by"))
    private Usuario creadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id", foreignKey = @ForeignKey(name = "fk_plantilla_campos_updated_by"))
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
