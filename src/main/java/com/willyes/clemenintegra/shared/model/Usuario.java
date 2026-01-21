package com.willyes.clemenintegra.shared.model;

import com.willyes.clemenintegra.shared.model.enums.NivelAccesoAdmin;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_acceso_admin", nullable = false, length = 20)
    @Builder.Default
    private NivelAccesoAdmin nivelAccesoAdmin = NivelAccesoAdmin.FULL;

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

    public NivelAccesoAdmin getNivelAccesoAdmin() {
        return nivelAccesoAdmin != null ? nivelAccesoAdmin : NivelAccesoAdmin.FULL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
