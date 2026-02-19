package com.willyes.clemenintegra.support;

import java.util.Arrays;
import java.util.LinkedHashSet;
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
        return SecurityMockMvcRequestPostProcessors.authentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        username,
                        "N/A",
                        toGrantedAuthorities(authorities)
                )
        );
    }

    public static UserRequestPostProcessor authWithRoles(String username, String... roles) {
        GrantedAuthority[] grantedAuthorities = toGrantedAuthorities(roles);
        return SecurityMockMvcRequestPostProcessors.user(username).authorities(grantedAuthorities);
    }

    public static RequestPostProcessor authWithPermissionsAndRoles(String username, String[] authorities, String[] roles) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(Arrays.asList(authorities));
        merged.addAll(Arrays.asList(roles));
        return auth(username, merged.toArray(String[]::new));
    }

    private static GrantedAuthority[] toGrantedAuthorities(String... values) {
        return Arrays.stream(values)
                .map(SimpleGrantedAuthority::new)
                .toArray(GrantedAuthority[]::new);
    }
}
