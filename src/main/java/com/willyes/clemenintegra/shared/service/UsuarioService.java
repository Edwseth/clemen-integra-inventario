package com.willyes.clemenintegra.shared.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationCredentialsNotFoundException("No se encontró autenticación válida");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return usuarioRepository.findById(customUserDetails.getId())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        }
        if (principal instanceof UserDetails userDetails) {
            return usuarioRepository.findByNombreUsuario(userDetails.getUsername())
                    .orElseGet(() -> {
                        Usuario usuario = new Usuario();
                        usuario.setNombreUsuario(userDetails.getUsername());
                        usuario.setNombreCompleto(userDetails.getUsername());
                        return usuario;
                    });
        }
        throw new AuthenticationCredentialsNotFoundException("Principal de autenticación no soportado");
    }

    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public Usuario obtenerUsuarioSistema() {
        return usuarioRepository.findFirstByRolAndActivoTrueOrderByIdAsc(RolUsuario.ROL_SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("No se encontró un usuario activo con rol SUPER_ADMIN para tareas automáticas"));
    }

    public Usuario obtenerUsuarioSistemaJobVencimientos() {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase("SYSTEM")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "USUARIO_SISTEMA_NO_CONFIGURADO"
                ));
        if (!usuario.isActivo()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USUARIO_SISTEMA_NO_CONFIGURADO");
        }
        return usuario;
    }
}
