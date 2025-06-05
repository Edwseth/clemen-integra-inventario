package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos (si aplica alguno)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Productos: accesible solo por jefes y operarios de almacén
                        .requestMatchers("/api/productos/**").hasAnyRole(
                                RolUsuario.ROL_JEFE_ALMACENES.name(),
                                RolUsuario.ROL_ALMACENISTA.name()
                        )

                        // Movimientos: accesible por jefes de almacén y producción
                        .requestMatchers("/api/movimientos/**").hasAnyRole(
                                RolUsuario.ROL_JEFE_ALMACENES.name(),
                                RolUsuario.ROL_ALMACENISTA.name(),
                                RolUsuario.ROL_JEFE_PRODUCCION.name()
                        )

                        // Calidad: accesible por analistas, microbiólogos y jefes de calidad
                        .requestMatchers("/api/calidad/**").hasAnyRole(
                                RolUsuario.ROL_JEFE_CALIDAD.name(),
                                RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                                RolUsuario.ROL_MICROBIOLOGO.name()
                        )

                        // Producción: accesible por líderes y jefe de producción
                        .requestMatchers("/api/produccion/**").hasAnyRole(
                                RolUsuario.ROL_JEFE_PRODUCCION.name(),
                                RolUsuario.ROL_LIDER_ALIMENTOS.name(),
                                RolUsuario.ROL_LIDER_HOMEOPATICOS.name()
                        )

                        // BOM: solo jefe de producción y líderes
                        .requestMatchers("/api/bom/**").hasAnyRole(
                                RolUsuario.ROL_JEFE_PRODUCCION.name(),
                                RolUsuario.ROL_LIDER_ALIMENTOS.name(),
                                RolUsuario.ROL_LIDER_HOMEOPATICOS.name()
                        )

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autorizado"))
                );

        return http.build();
    }
}

