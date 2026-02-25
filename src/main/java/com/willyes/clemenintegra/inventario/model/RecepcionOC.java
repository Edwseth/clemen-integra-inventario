package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recepciones_oc", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recepciones_oc_codigo", columnNames = "codigo"),
        @UniqueConstraint(name = "uk_recepciones_oc_orden_fecha", columnNames = {"orden_compra_id", "fecha_recepcion"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecepcionOC {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "codigo", nullable = false, length = 32)
    private String codigo;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDate fechaRecepcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_compra_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recepciones_oc_orden_compra"))
    private OrdenCompra ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_destino_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recepciones_oc_almacen_destino"))
    private Almacen almacenDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id",
            foreignKey = @ForeignKey(name = "fk_recepciones_oc_proveedor"))
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recepciones_oc_usuario"))
    private Usuario usuario;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Builder.Default
    @Column(name = "gastos_adicionales_total", precision = 19, scale = 6)
    private BigDecimal gastosAdicionalesTotal = BigDecimal.ZERO.setScale(6);

    @Builder.Default
    @Column(name = "criterio_prorrateo", length = 20)
    private String criterioProrrateo = "VALOR";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "recepcionOc", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecepcionOCDetalle> detalles = new ArrayList<>();

    public RecepcionOC(Long id) {
        this.id = id;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
