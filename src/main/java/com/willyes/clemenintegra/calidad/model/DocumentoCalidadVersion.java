package com.willyes.clemenintegra.calidad.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_calidad_versiones", indexes = {
        @Index(name = "idx_documentos_calidad_version_doc", columnList = "documento_id"),
        @Index(name = "idx_documentos_calidad_version", columnList = "documento_id, version")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoCalidadVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_documento_calidad_version_documento"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DocumentoCalidad documento;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "nombre_visible", length = 255)
    private String nombreVisible;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "hash_opcional", length = 128)
    private String hashOpcional;

    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
