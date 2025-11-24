package com.willyes.clemenintegra.planeacion.model;

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

    @Column(name = "fecha_ejecucion", nullable = false)
    private LocalDateTime fechaEjecucion;

    @Column(name = "horizonte_desde")
    private LocalDate horizonteDesde;

    @Column(name = "horizonte_hasta")
    private LocalDate horizonteHasta;

    @Column(name = "usuario_ejecuto_id")
    private Long usuarioEjecutoId;

    @Column(name = "resumen", length = 1000)
    private String resumen;

    @Builder.Default
    @OneToMany(mappedBy = "corrida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SugerenciaAbastecimiento> sugerencias = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaEjecucion == null) {
            fechaEjecucion = LocalDateTime.now();
        }
    }
}
