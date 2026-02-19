package com.willyes.clemenintegra.support;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;

public final class SecurityTestUtils {

    private SecurityTestUtils() {
    }

    public static UserRequestPostProcessor userWithAuthorities(String username, String... authorities) {
        return (UserRequestPostProcessor) TestAuth.auth(username, authorities);
    }
}
