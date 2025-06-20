package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "ordenes_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "codigo_orden", nullable = false, unique = true)
    private String codigoOrden;

    @Column(name = "fecha_orden", nullable = false)
    private LocalDate fechaOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orden_compra_proveedor"))
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 50, nullable = false)
    private EstadoOrdenCompra estado;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    public OrdenCompra(Long id) {
        this.id = id;
    }
}