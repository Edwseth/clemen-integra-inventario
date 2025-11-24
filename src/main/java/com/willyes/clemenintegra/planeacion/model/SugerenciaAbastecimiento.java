package com.willyes.clemenintegra.planeacion.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.OrigenSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sugerencias_abastecimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SugerenciaAbastecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corrida_mrp_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sug_corrida"))
    private CorridaMrp corrida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sug_producto"))
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoSugerenciaAbastecimiento tipo;

    @Column(name = "cantidad_sugerida", nullable = false, precision = 19, scale = 6)
    private BigDecimal cantidadSugerida;

    @Column(name = "fecha_requerida", nullable = false)
    private LocalDate fechaRequerida;

    @Column(name = "fecha_sugerida_pedido")
    private LocalDate fechaSugeridaPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoSugerenciaAbastecimiento estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 30)
    private OrigenSugerenciaAbastecimiento origen;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @Column(name = "orden_compra_id")
    private Long ordenCompraId;

    @Column(name = "orden_produccion_id")
    private Long ordenProduccionId;
}
