package com.willyes.clemenintegra.shared.repository;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    Optional<Usuario> findFirstByRolAndActivoTrueOrderByIdAsc(RolUsuario rol);

}
