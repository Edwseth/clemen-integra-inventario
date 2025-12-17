package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.TipoDocumentoOrdenCompra;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orden_compra_documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCompraDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_oc_documento_oc"))
    private OrdenCompra ordenCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 20, nullable = false)
    private TipoDocumentoOrdenCompra tipoDocumento;

    @Column(name = "nombre_visible", nullable = false, length = 255)
    private String nombreVisible;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_oc_documento_usuario"))
    private Usuario creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
