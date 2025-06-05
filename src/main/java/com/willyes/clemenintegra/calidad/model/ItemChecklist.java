package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_checklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "checklist_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_checklist"))
    private ChecklistCalidad checklist;

    @Column(name = "descripcion_item", columnDefinition = "TEXT", nullable = false)
    private String descripcionItem;

    @Column(name = "cumple")
    private Boolean cumple = false;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_revision", nullable = false)
    private LocalDateTime fechaRevision;

    @ManyToOne
    @JoinColumn(name = "revisado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_revisado_por"))
    private Usuario revisadoPor;
}

