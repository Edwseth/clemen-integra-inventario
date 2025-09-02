package com.willyes.clemenintegra.produccion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaProduccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_etapa", nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private Integer secuencia;

    @ManyToOne
    @JoinColumn(name = "orden_produccion_id")
    private OrdenProduccion ordenProduccion;

    @OneToMany(mappedBy = "etapaProduccion")
    private List<DetalleEtapa> detalles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoEtapa estado = EstadoEtapa.PENDIENTE;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
}
