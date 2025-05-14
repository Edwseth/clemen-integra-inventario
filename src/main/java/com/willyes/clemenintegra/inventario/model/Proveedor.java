package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "proveedor", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "nit_cedula", nullable = false, unique = true, length = 20)
    private String identificacion;

    @Column(name = "telefono_contacto", length = 20)
    private String telefono;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(length = 150)
    private String direccion;

    @Column(name = "pagina_web", length = 150)
    private String paginaWeb;

    @Column(name = "nombre_contacto", nullable = false, length = 100)
    private String nombreContacto;

    @Column(nullable = false)
    private Boolean activo;

    public Proveedor(Long id) {
        this.id = id;
    }
}
