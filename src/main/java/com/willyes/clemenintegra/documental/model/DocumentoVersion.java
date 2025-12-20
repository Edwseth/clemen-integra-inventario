package com.willyes.clemenintegra.documental.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_versiones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_documentos_version", columnNames = {"documento_id", "numero_version"})
}, indexes = {
        @Index(name = "idx_documentos_version_documento", columnList = "documento_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_documentos_version_documento"))
    private Documento documento;

    @Column(name = "numero_version", nullable = false)
    private int numeroVersion;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "nombre_archivo_original", nullable = false, length = 255)
    private String nombreArchivoOriginal;

    @Column(name = "nombre_visible", nullable = false, length = 255)
    private String nombreVisible;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "comentarios", columnDefinition = "TEXT")
    private String comentarios;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emitido_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_documentos_version_emitido_por"))
    private Usuario emitidoPor;

    @Builder.Default
    @Column(name = "vigente", nullable = false)
    private boolean vigente = true;
}
