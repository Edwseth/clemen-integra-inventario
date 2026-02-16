package com.willyes.clemenintegra.shared.security.service;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import com.willyes.clemenintegra.shared.repository.PermisoRepository;
import com.willyes.clemenintegra.shared.repository.RolRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioAuthoritiesService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioAuthoritiesService.class);

    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;

    public Collection<? extends GrantedAuthority> buildAuthorities(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return List.of();
        }

        RolUsuario rolEfectivo = resolveRolEfectivo(usuario);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority(rolEfectivo.name()));

        List<String> codigosPermiso = permisoRepository.findByRolesCodigoAndActivoTrue(rolEfectivo.name()).stream()
                .map(PermisoEntity::getCodigo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        authorities.addAll(codigosPermiso.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));

        return authorities;
    }

    private RolUsuario resolveRolEfectivo(Usuario usuario) {
        RolUsuario rolEnUsuario = usuario.getRol();
        if (usuario.getId() == null) {
            return rolEnUsuario;
        }

        List<String> rolesEnJoin = rolRepository.findCodigosByUsuarioId(usuario.getId());
        if (rolesEnJoin == null || rolesEnJoin.isEmpty()) {
            return rolEnUsuario;
        }

        if (rolesEnJoin.size() > 1) {
            log.warn("Usuario con más de un rol en usuarios_roles; se usa usuarios.rol. usuarioId={}, username={}, rolEnUsuario={}, rolesEnJoin={}",
                    usuario.getId(), usuario.getUsername(), rolEnUsuario.name(), rolesEnJoin);
            return rolEnUsuario;
        }

        String rolJoin = rolesEnJoin.get(0);
        if (!rolEnUsuario.name().equals(rolJoin)) {
            log.warn("Mismatch entre usuarios.rol y usuarios_roles; se usa usuarios.rol. usuarioId={}, username={}, rolEnUsuario={}, rolJoin={}",
                    usuario.getId(), usuario.getUsername(), rolEnUsuario.name(), rolJoin);
            return rolEnUsuario;
        }

        return rolEnUsuario;
    }
}
