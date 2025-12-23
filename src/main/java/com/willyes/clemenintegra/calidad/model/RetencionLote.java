package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "retencion_lote")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetencionLote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lote_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_retencion_lote"))
    private LoteProducto lote;

    @Column(name = "causa", columnDefinition = "TEXT", nullable = false)
    private String causa;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false,
            columnDefinition = "ENUM('RETENCION_CALIDAD','NO_CONFORMIDAD','NC_DOCUMENTAL','OTRO','REEVALUACION')")
    private MotivoRetencion motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "no_conformidad_id",
            foreignKey = @ForeignKey(name = "fk_retencion_no_conformidad"))
    private NoConformidad noConformidad;

    @Column(name = "fecha_retencion", nullable = false)
    private LocalDateTime fechaRetencion;

    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", columnDefinition = "ENUM('RETENIDO','LIBERADO')", nullable = false)
    private EstadoRetencion estado;

    @ManyToOne
    @JoinColumn(name = "aprobado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_retencion_usuario"))
    private Usuario aprobadoPor;
}
