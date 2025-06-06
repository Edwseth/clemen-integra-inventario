package com.willyes.clemenintegra.inventario.domain.model;

import com.willyes.clemenintegra.inventario.domain.enums.TipoMovimiento;
import jakarta.persistence.*;
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

    @Column(nullable = false, unique = true, length = 100)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 50)
    private TipoMovimiento tipoMovimiento;

    public MotivoMovimiento(Long id) {
        this.id = id;
    }
}
