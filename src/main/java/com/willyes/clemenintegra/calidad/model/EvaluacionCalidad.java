package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluaciones_calidad",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lote_tipo_evaluacion",
                columnNames = {"lotes_productos_id", "tipo_evaluacion"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionCalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false,
            columnDefinition = "ENUM('APROBADO','RECHAZADO','CONDICIONADO')")
    private ResultadoEvaluacion resultado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evaluacion", nullable = false,
            columnDefinition = "ENUM('FISICO_QUIMICO','MICROBIOLOGICO')")
    private TipoEvaluacion tipoEvaluacion;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDateTime fechaEvaluacion;

    @Column(name = "observaciones", columnDefinition = "TEXT", nullable = false)
    private String observaciones;

    @ElementCollection
    @CollectionTable(name = "archivos_evaluacion", joinColumns = @JoinColumn(name = "evaluacion_id"))
    @Column(name = "archivo")
    private java.util.List<String> archivosAdjuntos;

    @ManyToOne
    @JoinColumn(name = "lotes_productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eval_lote"))
    private LoteProducto loteProducto;

    @ManyToOne
    @JoinColumn(name = "usuarios_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eval_usuario"))
    private Usuario usuarioEvaluador;
}
