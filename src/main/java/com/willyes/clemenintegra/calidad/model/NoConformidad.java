package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.inventario.model.Usuario;
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

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "evidencia", length = 255)
    private String evidencia;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "usuario_reporta_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_no_conf_usuario_reporta"))
    private Usuario usuarioReporta;
}

