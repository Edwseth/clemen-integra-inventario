package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_cambios_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitacoraCambiosInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tabla_afectada", length = 45, nullable = false)
    private String tablaAfectada;

    @Column(name = "registro_id", nullable = false)
    private Long registroId;

    @Column(name = "campo_modificado", length = 45, nullable = false)
    private String campoModificado;

    @Column(name = "valor_ant", length = 255, nullable = false)
    private String valorAnt;

    @Column(name = "valor_nuevo", length = 255, nullable = false)
    private String valorNuevo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    @ManyToOne
    @JoinColumn(name = "usuarios_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bitacora_usuario"))
    private Usuario usuario;
}

