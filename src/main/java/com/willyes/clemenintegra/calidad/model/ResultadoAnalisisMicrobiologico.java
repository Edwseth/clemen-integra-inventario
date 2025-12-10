package com.willyes.clemenintegra.calidad.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "resultados_analisis_micro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoAnalisisMicrobiologico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_res_micro_eval"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EvaluacionCalidad evaluacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parametro_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_res_micro_param"))
    private ParametroAnalisisMicrobiologico parametro;

    @Column(name = "resultado", columnDefinition = "TEXT")
    private String resultado;

    @Column(name = "cumple")
    private Boolean cumple;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}

