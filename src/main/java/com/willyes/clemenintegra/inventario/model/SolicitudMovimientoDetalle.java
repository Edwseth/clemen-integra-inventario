package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "solicitudes_movimiento_detalle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SolicitudMovimientoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_movimiento_id", nullable = false)
    private SolicitudMovimiento solicitudMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteProducto lote;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "cantidad_atendida", precision = 18, scale = 6)
    private BigDecimal cantidadAtendida;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitudMovimientoDetalle estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_origen_id")
    private Almacen almacenOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_destino_id")
    private Almacen almacenDestino;

    @PrePersist
    @PreUpdate
    private void prePersist() {
        if (cantidadAtendida == null) {
            cantidadAtendida = BigDecimal.ZERO;
        }
        if (estado == null) {
            estado = EstadoSolicitudMovimientoDetalle.PENDIENTE;
        }
    }

    public LoteProducto getLote() {
        return lote;
    }
}

