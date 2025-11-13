package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidades_medida", uniqueConstraints = {
        @UniqueConstraint(name = "un_nombre_UNIQUE",  columnNames = "nombre"),
        @UniqueConstraint(name = "un_simbolo_UNIQUE", columnNames = "simbolo"),
        @UniqueConstraint(name = "un_codigo_UNIQUE",  columnNames = "codigo")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "nombre_plural", length = 60)
    private String nombrePlural;

    @Column(name = "simbolo", nullable = false, length = 5)
    private String simbolo;

    @Column(name="codigo", length=4)
    private String codigo;

    @Column(name="simbolo_impresion", length=10)
    private String simboloImpresion;

    public UnidadMedida(Long id) {
        this.id = id;
    }

    @PrePersist @PreUpdate
    private void normalize() {
        if (nombre != null) nombre = nombre.trim().toUpperCase();
        if (nombrePlural != null) nombrePlural = nombrePlural.trim().toUpperCase();
        if (simbolo != null) simbolo = simbolo.trim().toUpperCase();
        if (codigo != null) codigo = codigo.trim().toUpperCase();
    }
}

