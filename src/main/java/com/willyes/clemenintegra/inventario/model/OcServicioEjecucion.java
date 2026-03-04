package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "oc_servicio_ejecuciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcServicioEjecucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_compra_id")
    private OrdenCompra ordenCompra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_compra_detalle_id")
    private OrdenCompraDetalle ordenCompraDetalle;

    @Column(name = "cantidad_ejecutada", nullable = false, precision = 19, scale = 3)
    private BigDecimal cantidadEjecutada;

    @Column(name = "fecha_ejecucion", nullable = false)
    private LocalDateTime fechaEjecucion;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
