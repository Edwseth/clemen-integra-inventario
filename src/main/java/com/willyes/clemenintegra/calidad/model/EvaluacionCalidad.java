package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
            columnDefinition = "ENUM('APROBADO','RECHAZADO','CONDICIONADO')")
    private ResultadoEvaluacion resultado;

    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDateTime fechaEvaluacion;

    @Column(name = "observaciones", columnDefinition = "TEXT", nullable = false)
    private String observaciones;

    @Column(name = "archivo_adjunto", length = 255)
    private String archivoAdjunto;

    @ManyToOne
    @JoinColumn(name = "lotes_productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eval_lote"))
    private LoteProducto loteProducto;

    @ManyToOne
    @JoinColumn(name = "usuarios_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eval_usuario"))
    private Usuario usuarioEvaluador;
}
