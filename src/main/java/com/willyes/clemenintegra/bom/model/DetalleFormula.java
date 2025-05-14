package com.willyes.clemenintegra.bom.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleFormula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "formula_id")
    private FormulaProducto formula;

    @ManyToOne
    @JoinColumn(name = "insumo_id")
    private Producto insumo;

    @ManyToOne
    @JoinColumn(name = "unidad_medida_id")
    private UnidadMedida unidadMedida;

    @Column(name = "cantidad_necesaria")
    private Double cantidadNecesaria;

    private Boolean obligatorio;
}
