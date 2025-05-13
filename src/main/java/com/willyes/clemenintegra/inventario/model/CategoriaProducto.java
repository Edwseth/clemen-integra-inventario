package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorias_producto", uniqueConstraints = {
        @UniqueConstraint(name = "un_nombre_categoria_UNIQUE", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoCategoria tipo;

    public CategoriaProducto(Long id) {
        this.id = id;
    }
}

