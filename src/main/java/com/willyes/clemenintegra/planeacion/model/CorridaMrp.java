package com.willyes.clemenintegra.planeacion.model;

import com.willyes.clemenintegra.planeacion.model.enums.EstadoCorridaMrp;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "corridas_mrp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorridaMrp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanProduccionSemanal planProduccionSemanal;

    @Column(name = "fecha_ejecucion", nullable = false)
    private LocalDateTime fechaEjecucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_ejecucion_id")
    private Usuario usuarioEjecucion;

    @Column(name = "horizonte_inicio")
    private LocalDate horizonteInicio;

    @Column(name = "horizonte_fin")
    private LocalDate horizonteFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30)
    private EstadoCorridaMrp estado;

    @Column(name = "version_formula_usada", length = 100)
    private String versionFormulaUsada;

    @Builder.Default
    @OneToMany(mappedBy = "corrida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCorridaMrp> detalles = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaEjecucion == null) {
            fechaEjecucion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoCorridaMrp.EN_PROCESO;
        }
    }
}
