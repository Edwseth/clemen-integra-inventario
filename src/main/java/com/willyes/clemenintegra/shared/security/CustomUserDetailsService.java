package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        if (!usuario.isActivo()) {
            throw new DisabledException("El usuario está inactivo");
        }

        return User.builder()
                .username(usuario.getNombreUsuario())
                .password(usuario.getClave())
                .roles(usuario.getRol().name().replace("ROL_", ""))
                .accountLocked(usuario.isBloqueado())
                .disabled(!usuario.isActivo())
                .build();
    }
}

