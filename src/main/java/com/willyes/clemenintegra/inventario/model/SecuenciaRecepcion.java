package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "secuencias_recepcion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_secuencias_recepcion_anio_prefijo", columnNames = {"anio", "prefijo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuenciaRecepcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "prefijo", nullable = false, length = 16)
    private String prefijo;

    @Column(name = "secuencia_actual", nullable = false)
    private Long secuenciaActual;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    public void prePersist() {
        if (actualizadoEn == null) {
            actualizadoEn = LocalDateTime.now();
        }
    }
}
