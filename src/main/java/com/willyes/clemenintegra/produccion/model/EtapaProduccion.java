package com.willyes.clemenintegra.produccion.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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
}
