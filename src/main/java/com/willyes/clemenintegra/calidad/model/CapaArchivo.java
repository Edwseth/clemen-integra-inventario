package com.willyes.clemenintegra.calidad.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "capa_archivos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapaArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capa_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_capa_archivos_capa"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Capa capa;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "nombre_visible")
    private String nombreVisible;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
