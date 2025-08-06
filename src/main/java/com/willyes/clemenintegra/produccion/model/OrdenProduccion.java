package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenProduccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loteProduccion;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @Column(nullable = false)
    private Integer cantidadProgramada;

    @Column(nullable = true)
    private Integer cantidadProducida;

    @Enumerated(EnumType.STRING)
    private EstadoProduccion estado;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @OneToMany(mappedBy = "ordenProduccion")
    private List<EtapaProduccion> etapas;
}