package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
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

