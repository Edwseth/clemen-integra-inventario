package com.willyes.clemenintegra.shared.security.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {
    public JwtAuthenticationToken(String token) {
        super(null, token);
    }
}

