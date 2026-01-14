package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.PicklistPtModoAsignacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "picklists_pt_lineas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PicklistPtLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picklist_id", nullable = false)
    private PicklistPt picklist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_asignacion", nullable = false, length = 20)
    private PicklistPtModoAsignacion modoAsignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_producto_id")
    private LoteProducto loteProducto;
}
