package com.willyes.clemenintegra.bom.model;

import com.willyes.clemenintegra.bom.model.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoFormula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    @Column(name = "ruta_archivo")
    private String rutaArchivo;

    @ManyToOne
    @JoinColumn(name = "formula_id")
    private FormulaProducto formula;
}
