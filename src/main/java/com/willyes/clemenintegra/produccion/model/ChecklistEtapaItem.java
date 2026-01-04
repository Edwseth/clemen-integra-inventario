package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.model.enums.EstadoChecklistItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "etapa_checklist_item",
        indexes = {
                @Index(name = "idx_checklist_etapa_id", columnList = "etapa_produccion_id"),
                @Index(name = "idx_checklist_etapa_estado", columnList = "etapa_produccion_id, estado")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistEtapaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_produccion_id", nullable = false)
    private EtapaProduccion etapaProduccion;

    @Column(name = "nombre_paso", nullable = false, length = 255)
    private String nombrePaso;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obligatorio = Boolean.FALSE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completado = Boolean.FALSE;

    @Column(name = "no_aplica", nullable = false)
    @Builder.Default
    private Boolean noAplica = Boolean.FALSE;

    @Column(name = "permitir_no_aplica", nullable = false)
    @Builder.Default
    private Boolean permitirNoAplica = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    @Builder.Default
    private EstadoChecklistItem estado = EstadoChecklistItem.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_id")
    private Usuario completedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private Usuario createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private Usuario updatedBy;

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
