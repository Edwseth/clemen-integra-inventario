package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.TipoChecklist;
import com.willyes.clemenintegra.inventario.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_calidad")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistCalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_checklist", nullable = false,
            columnDefinition = "ENUM('ALMACENAMIENTO','LIMPIEZA','EQUIPOS')")
    private TipoChecklist tipoChecklist;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "descripcion_general", columnDefinition = "TEXT")
    private String descripcionGeneral;

    @ManyToOne
    @JoinColumn(name = "creado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_checklist_creado_por"))
    private Usuario creadoPor;
}

