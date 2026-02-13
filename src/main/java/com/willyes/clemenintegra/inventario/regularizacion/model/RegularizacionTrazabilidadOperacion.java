package com.willyes.clemenintegra.inventario.regularizacion.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "regularizacion_trazabilidad_operacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_regularizacion_trazabilidad_idempotency", columnNames = "idempotency_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegularizacionTrazabilidadOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "orden_produccion_id", nullable = false)
    private Long ordenProduccionId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reg_trazabilidad_operacion_creado_por"))
    private Usuario creadoPor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resultado_json", columnDefinition = "json")
    private String resultadoJson;
}
