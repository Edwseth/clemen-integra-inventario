package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();
        try {
            Claims claims = jwtTokenService.extraerClaims(token);
            String username = claims.getSubject();

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            return new UsernamePasswordAuthenticationToken(
                    userDetails, token, userDetails.getAuthorities()
            );
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado para solicitud de {}", requestUsername(token));
            throw new BadCredentialsException("Token expirado", e);
        } catch (SignatureException e) {
            log.warn("Firma de token inválida");
            throw new BadCredentialsException("Firma del token inválida", e);
        } catch (UsernameNotFoundException e) {
            log.warn("Usuario no encontrado en token");
            throw new BadCredentialsException("Usuario no encontrado", e);
        } catch (AuthenticationException e) {
            throw e;
        } catch (JwtException e) {
            log.warn("Token inválido");
            throw new BadCredentialsException("Token inválido", e);
        }
    }

    private String requestUsername(String token) {
        try {
            Claims claims = jwtTokenService.extraerClaims(token);
            return claims.getSubject();
        } catch (Exception ex) {
            return "desconocido";
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

