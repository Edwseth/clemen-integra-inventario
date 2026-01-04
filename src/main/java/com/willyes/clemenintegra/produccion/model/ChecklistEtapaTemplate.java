package com.willyes.clemenintegra.produccion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_etapa_template",
        indexes = @Index(name = "idx_cet_etapa_orden", columnList = "etapa_plantilla_id, orden"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistEtapaTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_plantilla_id", nullable = false)
    private EtapaPlantilla etapaPlantilla;

    @Column(name = "nombre_item", nullable = false, length = 255)
    private String nombreItem;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obligatorio = Boolean.FALSE;

    @Column(name = "permitir_no_aplica", nullable = false)
    @Builder.Default
    private Boolean permitirNoAplica = Boolean.FALSE;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
