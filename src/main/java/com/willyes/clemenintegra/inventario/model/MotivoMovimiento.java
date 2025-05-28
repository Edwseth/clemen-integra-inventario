package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "motivos_movimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MotivoMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 50)
    private TipoMovimiento tipoMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, unique = true, length = 50)
    private TipoMovimiento motivo;


   /* public MotivoMovimiento(Long id, String descripcion, TipoMovimiento tipoMovimiento, TipoMovimiento motivo) {
        this.id = id;
        this.descripcion = descripcion;
        this.tipoMovimiento = tipoMovimiento;
        this.motivo = motivo;
    }*/


}
