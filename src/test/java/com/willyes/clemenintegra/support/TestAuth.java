package com.willyes.clemenintegra.support;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor auth(String username, String... authorities) {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreUsuario(username)
                .clave("N/A")
                .nombreCompleto(username)
                .correo(username + "@test.local")
                .rol(RolUsuario.ROL_OPERARIO)
                .activo(true)
                .bloqueado(false)
                .build();
        CustomUserDetails principal = new CustomUserDetails(usuario, toGrantedAuthorities(authorities));
        return SecurityMockMvcRequestPostProcessors.authentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal,
                        "N/A",
                        toGrantedAuthorities(authorities)
                )
        );
    }

    public static UserRequestPostProcessor authWithRoles(String username, String... roles) {
        List<GrantedAuthority> grantedAuthorities = toGrantedAuthorities(roles);
        return SecurityMockMvcRequestPostProcessors.user(username).authorities(grantedAuthorities);
    }

    public static RequestPostProcessor authWithPermissionsAndRoles(String username, String[] authorities, String[] roles) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(Arrays.asList(authorities));
        merged.addAll(Arrays.asList(roles));
        return auth(username, merged.toArray(String[]::new));
    }

    private static List<GrantedAuthority> toGrantedAuthorities(String... values) {
        return Arrays.stream(values)
                .map(v -> (GrantedAuthority) new SimpleGrantedAuthority(v))
                .toList();
    }
}
