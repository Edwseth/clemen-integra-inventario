package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "picklists_pt_asignaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PicklistPtAsignacion {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_producto_id", nullable = false)
    private LoteProducto loteProducto;

    @Column(name = "cantidad_asignada", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidadAsignada;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(name = "almacen_id", nullable = false)
    private Integer almacenId;

    @Column(nullable = false)
    private Short orden;
}
