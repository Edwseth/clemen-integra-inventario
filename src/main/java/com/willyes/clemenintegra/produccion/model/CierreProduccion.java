package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cierres_produccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CierreProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_produccion_id", nullable = false)
    private OrdenProduccion ordenProduccion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCierre tipo;

    @Column(name = "cerrada_incompleta")
    private Boolean cerradaIncompleta;

    private String turno;
    private String observacion;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @PrePersist
    public void prePersist() {
        if (fechaCierre == null) {
            fechaCierre = LocalDateTime.now();
        }
    }
}
