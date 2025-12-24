package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.security.exception.SesionInactivaException;
import com.willyes.clemenintegra.shared.security.exception.SesionInvalidadaException;
import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final List<String> publicMatchers = List.of(
            "/api/auth/**",
            "/api/public/**",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/swagger-resources",
            "/configuration/ui",
            "/configuration/security",
            "/webjars/**",
            "/auth/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String uri = request.getRequestURI();

        if (shouldSkipFilter(request, uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticationManager authenticationManager = authenticationManagerProvider.getIfAvailable();
        if (authenticationManager == null) {
            log.warn("JwtAuthenticationFilter: no hay AuthenticationManager disponible, se omite validación JWT para {}", uri);
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
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("JwtAuthenticationFilter: URI {} excluida por método OPTIONS", uri);
            return true;
        }

        return publicMatchers.stream()
                .filter(pattern -> antPathMatcher.match(pattern, uri))
                .peek(pattern -> log.debug("JwtAuthenticationFilter: URI {} excluida por patrón público {}", uri, pattern))
                .findFirst()
                .isPresent();
    }
}
