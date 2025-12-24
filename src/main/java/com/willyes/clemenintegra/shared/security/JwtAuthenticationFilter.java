package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnBean(AuthenticationManager.class)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String uri = request.getRequestURI();

        if (shouldSkipFilter(request, uri)) {
            log.debug("JwtAuthenticationFilter: URI {} excluida, continúa la cadena", uri);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            try {
                log.debug("JwtAuthenticationFilter: token encontrado, delegando en AuthenticationManager para URI {}", uri);
                JwtAuthenticationToken authenticationRequest = new JwtAuthenticationToken(token);
                Authentication authentication = authenticationManager.authenticate(authenticationRequest);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (SesionInvalidadaException | SesionInactivaException | AuthenticationException ex) {
                SecurityContextHolder.clearContext();
                throw ex;
            }
        } else {
            log.debug("JwtAuthenticationFilter: sin token, continúa como anónimo para URI {}", uri);
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipFilter(HttpServletRequest request, String uri) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || uri.startsWith("/api/auth")
                || uri.startsWith("/api/public/")
                || "/auth/login".equals(uri);
    }
}
