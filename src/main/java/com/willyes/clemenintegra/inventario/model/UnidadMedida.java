package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidades_medida", uniqueConstraints = {
        @UniqueConstraint(name = "un_nombre_UNIQUE", columnNames = "nombre"),
        @UniqueConstraint(name = "un_simbolo_UNIQUE", columnNames = "simbolo")
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

    @Column(name = "simbolo", nullable = false, length = 5)
    private String simbolo;

    public UnidadMedida(Long id) {
        this.id = id;
    }

    public Long getId() {return id;}
    public String getNombre() {return nombre;}
    public String getSimbolo() {return simbolo;}
    public void setId(Long id) {this.id = id;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public void setSimbolo(String simbolo) {this.simbolo = simbolo;}
}

