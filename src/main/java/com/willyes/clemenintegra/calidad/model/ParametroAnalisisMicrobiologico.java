package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametros_analisis_micro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametroAnalisisMicrobiologico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_parametro_plantilla"))
    private PlantillaAnalisisMicrobiologico plantilla;

    @Column(name = "nombre_ensayo", nullable = false, length = 255)
    private String nombreEnsayo;

    @Column(name = "unidad", length = 100)
    private String unidad;

    @Column(name = "especificacion", length = 255)
    private String especificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_resultado", nullable = false,
            columnDefinition = "ENUM('NUMERICO','PRESENCIA_AUSENCIA','TEXTO')")
    private TipoResultadoAnalisis tipoResultado;

    @Column(name = "orden_parametro")
    private Integer orden;
}

