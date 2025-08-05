package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.security.service.JwtAuthenticationToken;
import com.willyes.clemenintegra.shared.security.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
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
            throw new BadCredentialsException("Token expirado", e);
        } catch (SignatureException e) {
            throw new BadCredentialsException("Firma del token inválida", e);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Usuario no encontrado", e);
        } catch (AuthenticationException e) {
            throw e;
        } catch (JwtException e) {
            throw new BadCredentialsException("Token inválido", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

