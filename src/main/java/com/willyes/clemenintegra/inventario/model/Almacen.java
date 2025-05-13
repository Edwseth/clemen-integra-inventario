package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.CategoriaAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "almacenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Almacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private CategoriaAlmacen categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoAlmacen tipo;

    public Almacen(Long id) {
        this.id = id;
    }
}

