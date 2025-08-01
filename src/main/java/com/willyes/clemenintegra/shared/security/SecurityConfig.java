package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final UsuarioInactivoFilter usuarioInactivoFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/swagger-resources",
                                "/configuration/ui",
                                "/configuration/security",
                                "/webjars/**",
                                "/api/calidad/evaluaciones/archivo/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/productos/**", "/api/ordenes-compra/**",
                                "/api/motivos/**", "/api/lotes/**", "/api/almacenes/**",
                                "/api/proveedores/**", "/api/unidades/**",
                                "/api/inventario/bitacora/**",
                                "/api/inventario/historial-ordenes/**",
                                "/api/inventario/ordenes-compra-detalle/**",
                                "/api/inventario/tipos-movimiento-detalle/**"
                        ).hasAnyAuthority(
                                RolUsuario.ROL_ALMACENISTA.name(),
                                RolUsuario.ROL_JEFE_ALMACENES.name(),
                                RolUsuario.ROL_CONTADOR.name(),
                                RolUsuario.ROL_JEFE_CALIDAD.name(),
                                RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                                RolUsuario.ROL_MICROBIOLOGO.name(),
                                RolUsuario.ROL_SUPER_ADMIN.name()
                        )
                        .requestMatchers("/api/movimientos/**", "/api/categorias/**",
                                "/api/inventario/alertas/**").hasAnyAuthority(
                                RolUsuario.ROL_JEFE_ALMACENES.name(),
                                RolUsuario.ROL_ALMACENISTA.name(),
                                RolUsuario.ROL_JEFE_PRODUCCION.name(),
                                RolUsuario.ROL_SUPER_ADMIN.name()
                        )
                        .requestMatchers("/api/calidad/**", "/api/lotes/**").hasAnyAuthority(
                                RolUsuario.ROL_JEFE_CALIDAD.name(),
                                RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                                RolUsuario.ROL_MICROBIOLOGO.name(),
                                RolUsuario.ROL_SUPER_ADMIN.name()
                        )
                        .requestMatchers("/api/produccion/calidad/**").hasAnyAuthority(
                                RolUsuario.ROL_JEFE_CALIDAD.name(),
                                RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                                RolUsuario.ROL_MICROBIOLOGO.name(),
                                RolUsuario.ROL_SUPER_ADMIN.name()
                        )
                        .requestMatchers("/api/produccion/**").hasAnyAuthority(
                                RolUsuario.ROL_JEFE_PRODUCCION.name(),
                                RolUsuario.ROL_LIDER_ALIMENTOS.name(),
                                RolUsuario.ROL_LIDER_HOMEOPATICOS.name(),
                                RolUsuario.ROL_SUPER_ADMIN.name()
                        )
                        .requestMatchers("/api/bom/**").hasAnyAuthority(
                                RolUsuario.ROL_JEFE_PRODUCCION.name(),
                                RolUsuario.ROL_JEFE_CALIDAD.name(),
                                RolUsuario.ROL_SUPER_ADMIN.name()
                        )
                        .requestMatchers("/api/inventario/ajustes/**").hasAnyAuthority(
                                RolUsuario.ROL_CONTADOR.name(),
                                RolUsuario.ROL_SUPER_ADMIN.name()
                        )
                        .requestMatchers("/api/reportes/**").hasAnyAuthority(
                                RolUsuario.ROL_ALMACENISTA.name(),
                                RolUsuario.ROL_JEFE_ALMACENES.name(),
                                RolUsuario.ROL_SUPER_ADMIN.name()
                        )
                        .requestMatchers("/api/**").hasAuthority(RolUsuario.ROL_SUPER_ADMIN.name())

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autorizado"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(usuarioInactivoFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
