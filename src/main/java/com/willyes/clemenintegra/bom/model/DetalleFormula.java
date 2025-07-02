package com.willyes.clemenintegra.bom.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
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
    private BigDecimal cantidadNecesaria;

    private Boolean obligatorio;

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public FormulaProducto getFormula() {return formula;}
    public void setFormula(FormulaProducto formula) {this.formula = formula;}
    public Producto getInsumo() {return insumo;}
    public void setInsumo(Producto insumo) {this.insumo = insumo;}
    public UnidadMedida getUnidadMedida() {return unidadMedida;}
    public void setUnidadMedida(UnidadMedida unidadMedida) {this.unidadMedida = unidadMedida;}
    public BigDecimal getCantidadNecesaria() {return cantidadNecesaria;}
    public void setCantidadNecesaria(BigDecimal cantidadNecesaria) {this.cantidadNecesaria = cantidadNecesaria;}
    public Boolean getObligatorio() {return obligatorio;}
    public void setObligatorio(Boolean obligatorio) {this.obligatorio = obligatorio;}
}
