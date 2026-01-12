package com.willyes.clemenintegra.planeacion.model;

import com.willyes.clemenintegra.planeacion.model.enums.EstadoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "sugerencias_abastecimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SugerenciaAbastecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_corrida_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sug_detalle"))
    private DetalleCorridaMrp detalleCorrida;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoSugerenciaAbastecimiento tipo;

    @Column(name = "cantidad_sugerida", precision = 19, scale = 6, nullable = false)
    private BigDecimal cantidadSugerida;

    @Column(name = "fecha_necesidad", nullable = false)
    private LocalDate fechaNecesidad;

    @Column(name = "fecha_sugerida_lanzamiento")
    private LocalDate fechaSugeridaLanzamiento;

    @Column(name = "lead_time_dias")
    private Integer leadTimeDias;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoSugerenciaAbastecimiento estado;

    @Column(name = "orden_compra_id")
    private Long ordenCompraId;

    @Column(name = "orden_produccion_id")
    private Long ordenProduccionId;

    @Transient
    private BigDecimal consumoTotalPeriodo;

    @Transient
    private BigDecimal consumoSemanalPromedio;

    @Transient
    private BigDecimal semanasCobertura;

    @Transient
    private String nivelCriticidad;

    @Transient
    private Boolean esCritico;

    @Builder.Default
    @Transient
    private List<String> razonesCriticidad = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SugerenciaAbastecimiento that = (SugerenciaAbastecimiento) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
