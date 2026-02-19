package com.willyes.clemenintegra.support;

import java.util.Arrays;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor auth(String username, String... authorities) {
        GrantedAuthority[] grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toArray(GrantedAuthority[]::new);
        return SecurityMockMvcRequestPostProcessors.user(username)
                .authorities(grantedAuthorities);
    }

    public static UserRequestPostProcessor authWithRoles(String username, String... roles) {
        return SecurityMockMvcRequestPostProcessors.user(username).roles(roles);
    }
}
