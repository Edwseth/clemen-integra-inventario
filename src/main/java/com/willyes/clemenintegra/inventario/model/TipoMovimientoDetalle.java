package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_movimiento_detalle", uniqueConstraints = {
        @UniqueConstraint(columnNames = "descripcion")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoMovimientoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false, unique = true)
    private String descripcion;
}

