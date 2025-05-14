package com.willyes.clemenintegra.produccion.model;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.Usuario;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccionSimple;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_lote", nullable = false, length = 45)
    private String codigoLote;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoProduccionSimple estado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuarios_id")
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "productos_id")
    private Producto producto;
}

