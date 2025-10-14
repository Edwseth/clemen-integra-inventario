package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "no_conformidad",
        uniqueConstraints = @UniqueConstraint(columnNames = "codigo")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoConformidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", length = 100, nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, columnDefinition = "ENUM('PRODUCTO','LOTE','PROVEEDOR','PROCESO')")
    private OrigenNoConformidad origen;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad", nullable = false, columnDefinition = "ENUM('CRITICA','MAYOR','MENOR')")
    private SeveridadNoConformidad severidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "ENUM('ABIERTA','CERRADA')")
    private EstadoNoConformidad estado;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "evidencia", length = 255)
    private String evidencia;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @ManyToOne
    @JoinColumn(name = "usuario_reporta_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_no_conf_usuario_reporta"))
    private Usuario usuarioReporta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id",
            foreignKey = @ForeignKey(name = "fk_nc_lote"))
    private LoteProducto lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id",
            foreignKey = @ForeignKey(name = "fk_nc_producto"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id",
            foreignKey = @ForeignKey(name = "fk_nc_evaluacion"))
    private EvaluacionCalidad evaluacion;

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}

