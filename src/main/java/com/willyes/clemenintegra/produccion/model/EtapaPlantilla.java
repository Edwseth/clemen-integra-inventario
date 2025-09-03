package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaPlantilla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private Integer secuencia;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
