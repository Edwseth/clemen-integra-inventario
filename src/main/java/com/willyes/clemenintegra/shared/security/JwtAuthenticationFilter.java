package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Authentication authRequest = new JwtAuthenticationToken(token);
                Authentication authResult = jwtAuthenticationProvider.authenticate(authRequest);

                SecurityContextHolder.getContext().setAuthentication(authResult);

                // ✅ Aquí sí es seguro poner el log
                log.info("Authorities asignadas: {}", authResult.getAuthorities());
            } catch (AuthenticationException ex) {
                log.warn("Autenticación JWT fallida en {}: {}", request.getRequestURI(), ex.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
                return;
            } catch (Exception ex) {
                log.error("Error procesando JWT en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error de autenticación");
                return;
            }
        } else {
            log.debug("Encabezado Authorization no encontrado o malformado");
        }

        filterChain.doFilter(request, response);
    }

}


