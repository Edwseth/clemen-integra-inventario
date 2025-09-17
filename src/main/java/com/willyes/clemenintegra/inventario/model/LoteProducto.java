package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.Produccion;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes_productos", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_codigo_lote_producto_almacen",
                columnNames = {"codigo_lote", "productos_id", "almacenes_id"}
        )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "codigo_lote", nullable = false, length = 100)
    private String codigoLote;

    @Column(name = "fecha_fabricacion")
    private LocalDateTime fechaFabricacion;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(name = "stock_lote", nullable = false, precision = 10, scale = 2)
    private BigDecimal stockLote;

    @Builder.Default
    @Column(name = "agotado", nullable = false)
    private boolean agotado = false;

    @Builder.Default
    @NotNull
    @Column(name = "stock_reservado", nullable = false, precision = 18, scale = 6)
    private BigDecimal stockReservado = BigDecimal.ZERO.setScale(6);

    @Column(name = "fecha_agotado")
    private LocalDateTime fechaAgotado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoLote estado;

    @Column(name = "temperatura_almacenamiento")
    private Double temperaturaAlmacenamiento;

    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_lotes_productos_producto"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacenes_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_lotes_productos_almacen"))
    private Almacen almacen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuarios_liberador_id",
            foreignKey = @ForeignKey(name = "fk_lotes_productos_usuario_liberador"))
    private Usuario usuarioLiberador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_produccion_id")
    private OrdenProduccion ordenProduccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produccion_id")
    private Produccion produccion;

    public LoteProducto(Long id) {
        this.id = id;
    }

    @PrePersist
    @PreUpdate
    private void normalizeDefaults() {
        if (stockReservado == null) {
            stockReservado = BigDecimal.ZERO.setScale(6);
        } else {
            stockReservado = stockReservado.setScale(6, RoundingMode.DOWN);
        }

        if (stockLote != null) {
            stockLote = stockLote.setScale(2, RoundingMode.DOWN);
        }
    }
}


