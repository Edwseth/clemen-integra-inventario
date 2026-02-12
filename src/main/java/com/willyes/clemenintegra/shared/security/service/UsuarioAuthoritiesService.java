package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        if (usuario == null || usuario.getRol() == null) {
            return List.of();
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority(usuario.getRol().name()));

        List<String> codigosPermiso = usuario.getId() != null
                ? permisoRepository.findCodigosPermisosActivosByUsuarioId(usuario.getId())
                : List.of();

        if (codigosPermiso == null || codigosPermiso.isEmpty()) {
            codigosPermiso = permisoRepository.findByRolesCodigoAndActivoTrue(usuario.getRol().name()).stream()
                    .map(PermisoEntity::getCodigo)
                    .toList();
        }

        authorities.addAll(codigosPermiso.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));

        return authorities;
    }
}
