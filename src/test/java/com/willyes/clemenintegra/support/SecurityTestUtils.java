package com.willyes.clemenintegra.support;

import java.util.Arrays;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;

public final class SecurityTestUtils {

    private SecurityTestUtils() {
    }

    public static UserRequestPostProcessor userWithAuthorities(String username, String... authorities) {
        GrantedAuthority[] grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toArray(GrantedAuthority[]::new);
        return SecurityMockMvcRequestPostProcessors.user(username)
                .authorities(grantedAuthorities);
    }
}
