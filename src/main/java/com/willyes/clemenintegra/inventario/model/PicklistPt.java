package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "picklists_pt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PicklistPt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "cliente_nombre", nullable = false, length = 255)
    private String clienteNombre;

    @Column(name = "min_vida_util_dias")
    private Integer minVidaUtilDias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PicklistPtEstado estado;

    @Column(name = "almacen_pt_id", nullable = false)
    private Integer almacenPtId;

    @Column(name = "tipo_movimiento_detalle_id", nullable = false)
    private Integer tipoMovimientoDetalleId;

    @Column(name = "doc_referencia", length = 100)
    private String docReferencia;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false)
    private Usuario creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmado_por")
    private Usuario confirmadoPor;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejecutado_por")
    private Usuario ejecutadoPor;

    @Column(name = "fecha_ejecucion")
    private LocalDateTime fechaEjecucion;

    @OneToMany(mappedBy = "picklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PicklistPtLinea> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "picklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PicklistPtAsignacion> asignaciones = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = PicklistPtEstado.BORRADOR;
        }
    }
}
