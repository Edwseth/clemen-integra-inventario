package com.willyes.clemenintegra.inventario.regularizacion.model;

import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "regularizacion_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegularizacionTrazabilidadDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regularizacion_id", nullable = false)
    private RegularizacionTrazabilidad regularizacion;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "lote_id", nullable = false)
    private Long loteId;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo;

    @Column(name = "almacen_origen_id")
    private Integer almacenOrigenId;

    @Column(name = "almacen_destino_id")
    private Integer almacenDestinoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_id", nullable = false)
    private MovimientoInventario movimiento;

    @PrePersist
    @PreUpdate
    private void normalizeCantidad() {
        if (cantidad != null) {
            cantidad = cantidad.setScale(6, java.math.RoundingMode.HALF_UP);
        }
    }
}
