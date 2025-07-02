package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mov", nullable = false, length = 20)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "fecha_ingreso", nullable = false, updatable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "doc_referencia", length = 45)
    private String docReferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_registrado_por"))
    private Usuario registradoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_productos1"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lotes_productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_lotes_productos1"))
    private LoteProducto lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacenes_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_almacenes1"))
    private Almacen almacen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedores_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_proveedores1"))
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordenes_compra_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_ordenes_compra1"))
    private OrdenCompra ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivos_movimiento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_motivos_movimiento1"))
    private MotivoMovimiento motivoMovimiento;

    @ManyToOne
    @JoinColumn(name = "tipos_movimiento_detalle_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mov_inv_tipo_mov_detalle"))
    private TipoMovimientoDetalle tipoMovimientoDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_detalle_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_orden_compra_detalle"))
    private OrdenCompraDetalle ordenCompraDetalle;

    @PrePersist
    public void prePersist() {
        if (this.fechaIngreso == null) {
            this.fechaIngreso = LocalDateTime.now();
        }
    }

    public TipoMovimiento getTipoMovimiento() {return tipoMovimiento;}
    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {this.tipoMovimiento = tipoMovimiento;}
    public LocalDateTime getFechaIngreso() {return fechaIngreso;}
    public void setFechaIngreso(LocalDateTime fechaIngreso) {this.fechaIngreso = fechaIngreso;}
}



