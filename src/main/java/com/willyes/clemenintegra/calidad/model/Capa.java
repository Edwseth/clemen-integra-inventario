package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.*;
import com.willyes.clemenintegra.inventario.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "capa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Capa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "no_conformidad_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_capa_no_conformidad"))
    private NoConformidad noConformidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, columnDefinition = "ENUM('CORRECTIVA','PREVENTIVA')")
    private TipoCapa tipo;

    @ManyToOne
    @JoinColumn(name = "responsable_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_capa_responsable"))
    private Usuario responsable;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", columnDefinition = "ENUM('ACTIVA','CERRADA','VENCIDA')", nullable = false)
    private EstadoCapa estado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}

