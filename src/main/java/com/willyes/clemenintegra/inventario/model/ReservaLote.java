package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas_lote", indexes = {
        @Index(name = "idx_reservas_lote_lote_id", columnList = "lote_id"),
        @Index(name = "idx_reservas_lote_detalle_id", columnList = "solicitud_movimiento_detalle_id"),
        @Index(name = "idx_reservas_lote_lote_estado", columnList = "lote_id, estado")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReservaLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservas_lote_lote"))
    private LoteProducto lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_movimiento_detalle_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservas_lote_detalle"))
    private SolicitudMovimientoDetalle solicitudMovimientoDetalle;

    @Column(name = "cantidad_reservada", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidadReservada;

    @Builder.Default
    @Column(name = "cantidad_consumida", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidadConsumida = BigDecimal.ZERO.setScale(6);

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReservaLote estado = EstadoReservaLote.ACTIVA;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        normalize();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (cantidadReservada != null) {
            cantidadReservada = cantidadReservada.setScale(6, RoundingMode.HALF_UP);
        }
        if (cantidadConsumida == null) {
            cantidadConsumida = BigDecimal.ZERO.setScale(6);
        } else {
            cantidadConsumida = cantidadConsumida.setScale(6, RoundingMode.HALF_UP);
        }
        if (cantidadReservada != null && cantidadConsumida.compareTo(cantidadReservada) > 0) {
            cantidadConsumida = cantidadReservada;
        }
        if (estado == null) {
            estado = EstadoReservaLote.ACTIVA;
        }
    }
}

