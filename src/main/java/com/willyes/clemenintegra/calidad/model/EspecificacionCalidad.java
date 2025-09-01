package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Column(name = "valor_minimo", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorMaximo;

    @Column(name = "unidad", length = 45, nullable = false)
    private String unidad;

    @Column(name = "metodo_ensayo", length = 100, nullable = false)
    private String metodoEnsayo;

    @ManyToOne
    @JoinColumn(name = "productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_espec_calidad_producto"))
    private Producto producto;
}

