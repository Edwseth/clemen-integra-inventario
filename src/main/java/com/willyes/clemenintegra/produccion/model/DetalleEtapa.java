package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleEtapa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "orden_produccion_id")
    private OrdenProduccion ordenProduccion;

    @ManyToOne
    @JoinColumn(name = "etapa_id")
    private EtapaProduccion etapaProduccion;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario operario;
}
