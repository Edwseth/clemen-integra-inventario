package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.EstadoCondicionUso;
import com.willyes.clemenintegra.calidad.model.enums.TipoCondicionUso;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "condiciones_uso", indexes = {
        @Index(name = "idx_condicion_uso_lote_estado", columnList = "lote_id, estado")
})
public class CondicionUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_condicion_uso_lote"))
    private LoteProducto lote;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false,
            columnDefinition = "ENUM('REEVALUACION_FECHA','DISTRIBUCION_CONDICIONADA','LIMITACION_USO','OTRO')")
    private TipoCondicionUso tipo;

    @Column(name = "parametro_fecha")
    private LocalDateTime parametroFecha;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false,
            columnDefinition = "ENUM('ACTIVA','LEVANTADA')")
    private EstadoCondicionUso estado;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
