package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conteos_ciclicos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conteos_ciclicos_idempotency", columnNames = "idempotency_key_aplicar")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConteoCiclico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conteos_ciclicos_almacen"))
    private Almacen almacen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoConteoCiclico estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conteos_ciclicos_creado_por"))
    private Usuario creadoPor;

    @Column(name = "aplicado_en")
    private LocalDateTime aplicadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aplicado_por_id",
            foreignKey = @ForeignKey(name = "fk_conteos_ciclicos_aplicado_por"))
    private Usuario aplicadoPor;

    @Column(name = "idempotency_key_aplicar", length = 64)
    private String idempotencyKeyAplicar;

    @Builder.Default
    @OneToMany(mappedBy = "conteo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConteoCiclicoDetalle> detalles = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoConteoCiclico.BORRADOR;
        }
    }
}
