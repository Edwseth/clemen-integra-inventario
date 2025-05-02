package com.willyes.clemenintegra.inventario.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lotes_productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "productos_id", nullable = false)
    private Producto producto;

    // Agrega otros campos cuando vayas a construir esta entidad
}

