package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "especificaciones_calidad")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspecificacionCalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parametro", length = 45, nullable = false)
    private String parametro;

    @Column(name = "valor_minimo", length = 45, nullable = false)
    private String valorMinimo;

    @Column(name = "valor_maximo", length = 45, nullable = false)
    private String valorMaximo;

    @Column(name = "metodo_ensayo", length = 100, nullable = false)
    private String metodoEnsayo;

    @ManyToOne
    @JoinColumn(name = "productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_espec_calidad_producto"))
    private Producto producto;
}

