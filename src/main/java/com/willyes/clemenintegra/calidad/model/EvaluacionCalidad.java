package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Evaluación de calidad asociada a un lote de producto (LoteProducto) y, por extensión,
 * al Producto del inventario. Aquí se registran resultados globales y se vinculan
 * análisis microbiológicos vía {@link ResultadoAnalisisMicrobiologico}.
 */
@Entity
@Table(name = "evaluaciones_calidad")
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
            columnDefinition = "ENUM('CONFORME','CONDICIONADO','NO_CONFORME')")
    private ResultadoEvaluacion resultado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evaluacion", nullable = false,
            columnDefinition = "ENUM('FISICO','QUIMICO_MICROBIOLOGICO')")
    private TipoEvaluacion tipoEvaluacion;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDateTime fechaEvaluacion;

    @Column(name = "observaciones", columnDefinition = "TEXT", nullable = false)
    private String observaciones;

    @ElementCollection
    @CollectionTable(name = "archivos_evaluacion", joinColumns = @JoinColumn(name = "evaluacion_id"))
    private java.util.List<ArchivoEvaluacion> archivosAdjuntos;

    @ManyToOne
    @JoinColumn(name = "lotes_productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eval_lote"))
    private LoteProducto loteProducto;

    @ManyToOne
    @JoinColumn(name = "usuarios_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eval_usuario"))
    private Usuario usuarioEvaluador;
}
