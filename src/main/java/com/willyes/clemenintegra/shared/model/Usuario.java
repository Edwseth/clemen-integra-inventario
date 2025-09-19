package com.willyes.clemenintegra.shared.model;

import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "un_nombre_usuario_UNIQUE", columnNames = "nombre_usuario"),
        // TODO: Reactivar la restricción de unicidad del correo al desplegar en producción.
        // @UniqueConstraint(name = "un_correo_usuario_UNIQUE", columnNames = "correo")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_usuario", nullable = false, length = 45)
    private String nombreUsuario;

    @Column(name = "clave", nullable = false, length = 80)
    private String clave;

    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    @Column(name = "correo", nullable = false, length = 45)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 50)
    private RolUsuario rol;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "bloqueado", nullable = false)
    private boolean bloqueado;

    @Column(name = "codigo_2fa", length = 6)
    private String codigo2FA;

    @Column(name = "codigo_2fa_expira_en")
    private LocalDateTime codigo2FAExpiraEn;

    public Usuario(Long id) {
        this.id = id;
    }
}

