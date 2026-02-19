package com.willyes.clemenintegra.support;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class SecurityTestUtils {

    private SecurityTestUtils() {
    }

    public static RequestPostProcessor userWithAuthorities(String username, String... authorities) {
        return TestAuth.auth(username, authorities);
    }
}
