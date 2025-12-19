package com.willyes.clemenintegra.calidad.model;

import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadEstado;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documentos_calidad", indexes = {
        @Index(name = "idx_documentos_calidad_tipo_estado", columnList = "tipo, estado"),
        @Index(name = "idx_documentos_calidad_codigo", columnList = "codigo"),
        @Index(name = "idx_documentos_calidad_nombre", columnList = "nombre"),
        @Index(name = "idx_documentos_calidad_lote", columnList = "lote_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoCalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private DocumentoCalidadTipo tipo;

    @Column(name = "codigo", length = 60)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private DocumentoCalidadEstado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id",
            foreignKey = @ForeignKey(name = "fk_documento_calidad_lote"))
    private LoteProducto lote;

    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "actualizado_por_id")
    private Long actualizadoPorId;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version DESC")
    @Builder.Default
    private List<DocumentoCalidadVersion> versiones = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = DocumentoCalidadEstado.VIGENTE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
