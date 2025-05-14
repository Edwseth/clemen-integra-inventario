package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.inventario.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlCalidadProceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String parametro;

    @Column(name = "valor_medido")
    private String valorMedido;

    @Column(nullable = true)
    private Boolean cumple;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "detalle_etapa_id")
    private DetalleEtapa detalleEtapa;

    @ManyToOne
    @JoinColumn(name = "evaluado_por_id")
    private Usuario evaluador;
}

