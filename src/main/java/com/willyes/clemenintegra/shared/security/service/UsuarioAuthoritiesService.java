package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioAuthoritiesService {

    private final PermisoRepository permisoRepository;

    public Collection<? extends GrantedAuthority> buildAuthorities(Usuario usuario) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (usuario == null || usuario.getRol() == null) {
            return authorities;
        }

        authorities.add(new SimpleGrantedAuthority(usuario.getRol().name()));

        List<GrantedAuthority> permisos = permisoRepository.findByRolesCodigoAndActivoTrue(usuario.getRol().name()).stream()
                .map(PermisoEntity::getCodigo)
                .filter(Objects::nonNull)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        authorities.addAll(permisos);

        return authorities;
    }
}
