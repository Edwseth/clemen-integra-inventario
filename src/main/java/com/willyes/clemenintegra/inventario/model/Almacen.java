package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
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
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_almacen", nullable = false, length = 30)
    private TipoCategoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_almacen", nullable = false, length = 30)
    private TipoAlmacen tipo;

    public Almacen(Integer id) {
        this.id = id;
    }
}

