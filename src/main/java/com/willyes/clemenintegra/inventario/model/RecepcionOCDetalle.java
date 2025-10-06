package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "recepciones_oc_detalles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecepcionOCDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recepcion_oc_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recepciones_oc_detalles_recepcion"))
    private RecepcionOC recepcionOc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_compra_detalle_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recepciones_oc_detalles_orden_compra_detalle"))
    private OrdenCompraDetalle ordenCompraDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recepciones_oc_detalles_producto"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lotes_productos_id",
            foreignKey = @ForeignKey(name = "fk_recepciones_oc_detalles_lote"))
    private LoteProducto lote;

    @Column(name = "cantidad_recibida", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidadRecibida;

    public RecepcionOCDetalle(Long id) {
        this.id = id;
    }
}
