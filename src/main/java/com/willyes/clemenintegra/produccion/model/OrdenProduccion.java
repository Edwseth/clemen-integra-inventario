package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.produccion.model.enums.EstadoBatchRecord;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenProduccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_orden", unique = true, nullable = false)
    private String codigoOrden;

    private String loteProduccion;

    @Column(name = "lote_producto_id")
    private Long loteId;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadProgramada;

    @Column(nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal cantidadProducida;

    @Column(name = "cantidad_producida_acumulada", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal cantidadProducidaAcumulada;

    @Column(name = "fecha_ultimo_cierre")
    private LocalDateTime fechaUltimoCierre;

    @Column(precision = 10, scale = 2)
    private BigDecimal porcentajeCumplimiento;

    @Enumerated(EnumType.STRING)
    private TipoCierre tipoCierre;

    @Column(name = "usuario_cierre_id")
    private Long usuarioCierreId;

    private LocalDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    private EstadoProduccion estado;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "unidad_medida_id")
    private UnidadMedida unidadMedida;

    @ManyToOne
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @OneToMany(mappedBy = "ordenProduccion")
    private List<EtapaProduccion> etapas;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "batch_record_estado", nullable = false)
    private EstadoBatchRecord batchRecordEstado = EstadoBatchRecord.BORRADOR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_record_revisado_por_id")
    private Usuario batchRecordRevisadoPor;

    @Column(name = "batch_record_fecha_revision")
    private LocalDateTime batchRecordFechaRevision;

    @Column(name = "batch_record_observaciones_calidad", columnDefinition = "TEXT")
    private String batchRecordObservacionesCalidad;

    @Transient
    private Long lotePsId;

    @Version
    private Long version;

    // Garantiza que las instancias construidas con builder no persistan un estado nulo
    // (causante del error de columna NOT NULL en batch_record_estado).
    @PrePersist
    public void prePersist() {
        if (batchRecordEstado == null) {
            batchRecordEstado = EstadoBatchRecord.BORRADOR;
        }
    }
}
