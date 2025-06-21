package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorias_producto", uniqueConstraints = {
        @UniqueConstraint(name = "un_nombre_categoria_UNIQUE", columnNames = "nombre")
})
@Data
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

    public Long getId() {return id;}
    public String getNombre() {return nombre;}
    public TipoCategoria getTipo() {return tipo;}
    public void setId(Long id) {this.id = id;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public void setTipo(TipoCategoria tipo) {this.tipo = tipo;}
}

