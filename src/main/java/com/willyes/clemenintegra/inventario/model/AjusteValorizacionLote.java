package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ajustes_valorizacion_lote", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ajuste_val_lote_idem", columnNames = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteValorizacionLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_aj_val_lote_lote"))
    private LoteProducto lote;

    @Column(name = "codigo_lote", nullable = false, length = 80)
    private String codigoLote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_aj_val_lote_producto"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_aj_val_lote_almacen"))
    private Almacen almacen;

    @Column(name = "costo_unitario_anterior", precision = 19, scale = 6, nullable = false)
    private BigDecimal costoUnitarioAnterior;

    @Column(name = "costo_unitario_nuevo", precision = 19, scale = 6, nullable = false)
    private BigDecimal costoUnitarioNuevo;

    @Column(name = "costo_total_anterior", precision = 19, scale = 6, nullable = false)
    private BigDecimal costoTotalAnterior;

    @Column(name = "costo_total_nuevo", precision = 19, scale = 6, nullable = false)
    private BigDecimal costoTotalNuevo;

    @Column(name = "total_ingresado_anterior", precision = 19, scale = 6, nullable = false)
    private BigDecimal totalIngresadoAnterior;

    @Column(name = "total_ingresado_nuevo", precision = 19, scale = 6, nullable = false)
    private BigDecimal totalIngresadoNuevo;

    @Column(name = "motivo", nullable = false, length = 120)
    private String motivo;

    @Column(name = "observacion", nullable = false, length = 500)
    private String observacion;

    @Column(name = "documento_soporte", nullable = false, length = 255)
    private String documentoSoporte;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "payload_fingerprint", nullable = false, length = 64)
    private String payloadFingerprint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_aj_val_lote_usuario"))
    private Usuario usuario;

    @Column(name = "fecha_ajuste", nullable = false)
    private LocalDateTime fechaAjuste;
}
