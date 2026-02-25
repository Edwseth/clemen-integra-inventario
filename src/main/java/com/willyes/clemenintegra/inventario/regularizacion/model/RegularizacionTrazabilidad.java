package com.willyes.clemenintegra.inventario.regularizacion.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "regularizaciones_trazabilidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegularizacionTrazabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_produccion_id", nullable = false)
    private Long ordenProduccionId;

    @Column(name = "cantidad_programada", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidadProgramada;

    @Column(name = "cantidad_real", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidadReal;

    @Column(name = "diferencia", nullable = false, precision = 18, scale = 6)
    private BigDecimal diferencia;

    @Column(name = "ajustar_pt", nullable = false)
    private boolean ajustarPt;

    @Column(name = "documento_referencia", length = 45)
    private String documentoReferencia;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso;

    @PrePersist
    @PreUpdate
    private void normalizeCantidad() {
        if (cantidadProgramada != null) cantidadProgramada = cantidadProgramada.setScale(6, java.math.RoundingMode.HALF_UP);
        if (cantidadReal != null) cantidadReal = cantidadReal.setScale(6, java.math.RoundingMode.HALF_UP);
        if (diferencia != null) diferencia = diferencia.setScale(6, java.math.RoundingMode.HALF_UP);
    }
}
