package com.willyes.clemenintegra.documental.model;

import com.willyes.clemenintegra.documental.model.enums.AreaDocumento;
import com.willyes.clemenintegra.documental.model.enums.EstadoDocumento;
import com.willyes.clemenintegra.documental.model.enums.TipoDocumento;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documentos", indexes = {
        @Index(name = "idx_documentos_tipo_area_estado", columnList = "tipo, area, estado"),
        @Index(name = "idx_documentos_codigo", columnList = "codigo"),
        @Index(name = "idx_documentos_nombre", columnList = "nombre")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_documentos_codigo", columnNames = "codigo")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDocumento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "area", nullable = false, length = 30)
    private AreaDocumento area;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDocumento estado;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_documentos_creado_por"))
    private Usuario creadoPor;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Builder.Default
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<DocumentoVersion> versiones = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoDocumento.EN_ELABORACION;
        }
    }
}
