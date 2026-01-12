package com.willyes.clemenintegra.shared.model;

import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "un_nombre_usuario_UNIQUE", columnNames = "nombre_usuario"),
        // TODO: Reactivar la restricción de unicidad del correo al desplegar en producción.
        // @UniqueConstraint(name = "un_correo_usuario_UNIQUE", columnNames = "correo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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

    @Column(name = "session_version", nullable = false)
    @Builder.Default
    private Long sessionVersion = 0L;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    public Usuario(Long id) {
        this.id = id;
    }
}
