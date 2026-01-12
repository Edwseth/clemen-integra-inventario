package com.willyes.clemenintegra.planeacion.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.model.enums.TipoCambioMrp;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "detalles_corrida_mrp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleCorridaMrp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corrida_id", nullable = false)
    private CorridaMrp corrida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "requerimiento_bruto", precision = 19, scale = 6, nullable = false)
    private BigDecimal requerimientoBruto;

    @Column(name = "inventario_disponible", precision = 19, scale = 6, nullable = false)
    private BigDecimal inventarioDisponible;

    @Column(name = "recepciones_programadas", precision = 19, scale = 6, nullable = false)
    private BigDecimal recepcionesProgramadas;

    @Column(name = "requerimiento_neto", precision = 19, scale = 6, nullable = false)
    private BigDecimal requerimientoNeto;

    @Column(name = "nivel_bom")
    private Integer nivelBom;

    @Column(name = "mensaje_validacion", length = 500)
    private String mensajeValidacion;

    @OneToOne(mappedBy = "detalleCorrida", cascade = CascadeType.ALL, orphanRemoval = true)
    private SugerenciaAbastecimiento sugerencia;

    @Transient
    private TipoCambioMrp tipoCambioMrp;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DetalleCorridaMrp that = (DetalleCorridaMrp) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
