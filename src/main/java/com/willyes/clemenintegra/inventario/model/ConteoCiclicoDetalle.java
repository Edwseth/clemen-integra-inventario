package com.willyes.clemenintegra.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "conteos_ciclicos_detalle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConteoCiclicoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conteo_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conteos_ciclicos_detalle_conteo"))
    private ConteoCiclico conteo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conteos_ciclicos_detalle_producto"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_producto_id",
            foreignKey = @ForeignKey(name = "fk_conteos_ciclicos_detalle_lote"))
    private LoteProducto loteProducto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_fisica_id",
            foreignKey = @ForeignKey(name = "fk_conteos_ciclicos_detalle_ubicacion"))
    private UbicacionFisica ubicacionFisica;

    @Column(name = "stock_sistema", nullable = false, precision = 10, scale = 2)
    private BigDecimal stockSistema;

    @Column(name = "conteo_fisico", nullable = false, precision = 10, scale = 2)
    private BigDecimal conteoFisico;

    @Column(name = "diferencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal diferencia;

    @PrePersist
    @PreUpdate
    public void ajustarEscalas() {
        if (stockSistema != null) {
            stockSistema = stockSistema.setScale(2, RoundingMode.HALF_UP);
        }
        if (conteoFisico != null) {
            conteoFisico = conteoFisico.setScale(2, RoundingMode.HALF_UP);
        }
        if (conteoFisico != null && stockSistema != null) {
            diferencia = conteoFisico.subtract(stockSistema).setScale(2, RoundingMode.HALF_UP);
        } else if (diferencia != null) {
            diferencia = diferencia.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
