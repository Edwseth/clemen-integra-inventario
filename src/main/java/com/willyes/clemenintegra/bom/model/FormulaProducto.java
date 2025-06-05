package com.willyes.clemenintegra.bom.model;

import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormulaProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private String version;

    @Enumerated(EnumType.STRING)
    private EstadoFormula estado;

    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL)
    private List<DetalleFormula> detalles;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL)
    private List<DocumentoFormula> documentos;
}

