package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_compra_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal iva;

    @Column(name = "cantidad_recibida", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadRecibida;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ordenes_compra_id")
    private OrdenCompra ordenCompra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "productos_id")
    private Producto producto;

    public OrdenCompraDetalle(Long id) {
        this.id = id;
    }
}

