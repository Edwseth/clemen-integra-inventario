package com.willyes.clemenintegra.inventario.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidades_medida", uniqueConstraints = {
        @UniqueConstraint(name = "un_nombre_UNIQUE", columnNames = "nombre"),
        @UniqueConstraint(name = "un_simbolo_UNIQUE", columnNames = "simbolo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "simbolo", nullable = false, length = 5)
    private String simbolo;

    public UnidadMedida(Long id) {
        this.id = id;
    }
}

