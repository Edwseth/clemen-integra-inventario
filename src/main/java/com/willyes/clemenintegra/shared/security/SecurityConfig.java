package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.model.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    //private final CustomUserDetailsService customUserDetailsService;
    private final UsuarioInactivoFilter usuarioInactivoFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    //private final UsuarioPrincipal usuarioPrincipal;

    //@Bean
    //public PasswordEncoder passwordEncoder() {
        //return new BCryptPasswordEncoder();
    //}

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    auth.requestMatchers(
                            "/auth/login",
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
                    ).permitAll();

                    auth.requestMatchers(
                            "/api/productos/**",
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
                            RolUsuario.ROL_COMPRADOR.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                            RolUsuario.ROL_MICROBIOLOGO.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/inventario/ordenes/**").hasAnyAuthority(
                            RolUsuario.ROL_COMPRADOR.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/ordenes-compra/**").hasAnyAuthority(
                            RolUsuario.ROL_COMPRADOR.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/movimientos/**", "/api/categorias/**",
                            "/api/inventario/alertas/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/calidad/**", "/api/lotes/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                            RolUsuario.ROL_MICROBIOLOGO.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/produccion/calidad/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                            RolUsuario.ROL_MICROBIOLOGO.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/produccion/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_LIDER_ALIMENTOS.name(),
                            RolUsuario.ROL_LIDER_HOMEOPATICOS.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/bom/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/inventario/ajustes/**").hasAnyAuthority(
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/reportes/**").hasAnyAuthority(
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(
                            "/api/inventarios/solicitudes/**",
                            "/api/inventario/solicitudes/**"
                    ).hasAnyAuthority(
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    // Mantener esta regla genérica al final
                    auth.requestMatchers("/api/**").authenticated();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                (request, response, authEx) -> {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"error\":\"No autorizado\"}");
                                }
                ))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(usuarioInactivoFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(List.of(
                "https://*.ngrok-free.app",
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Bypass-Auth-Redirect",
                "ngrok-skip-browser-warning"
        ));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> usuarioRepository
                .findByNombreUsuario(username.trim())
                .map(UsuarioPrincipal::new) // usa tu clase que implementa UserDetails
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
