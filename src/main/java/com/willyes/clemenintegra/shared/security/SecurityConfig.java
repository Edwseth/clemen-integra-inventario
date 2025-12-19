package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.model.UsuarioPrincipal;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioInactivoFilter usuarioInactivoFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Orígenes permitidos por perfil (lista separada por comas)
    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsProp;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    auth.requestMatchers(
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info",
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
                            "/api/public/**"
                    ).permitAll();

                    auth.requestMatchers(
                            "/api/productos/**",
                            "/api/motivos/**", "/api/lotes/**", "/api/almacenes/**",
                            "/api/proveedores/**", "/api/unidades/**",
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
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/movimientos/**", "/api/categorias/**",
                            "/api/inventario/alertas/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/recepciones/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    // 1) Lectura de planes semanales (Producción + Compras + Super Admin)
                    auth.requestMatchers(HttpMethod.GET, "/api/planeacion/planes-semanales/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_COMPRADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );
                    // 2) Resto de operaciones de planeación (crear/editar/eliminar):
                    auth.requestMatchers("/api/planeacion/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/mrp/**").hasAnyAuthority(
                            RolUsuario.ROL_COMPRADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.POST, "/api/ordenes-compra/*/documentos").hasAnyAuthority(
                            RolUsuario.ROL_COMPRADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.GET,
                            "/api/ordenes-compra/*/documentos",
                            "/api/ordenes-compra/documentos/**").hasAnyAuthority(
                            RolUsuario.ROL_COMPRADOR.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/calidad/capas", "/api/calidad/capas/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(
                            "/api/calidad/plantillas-micro/**",
                            "/api/calidad/evaluaciones/plantillas/micro/**",
                            "/api/calidad/evaluaciones/*/resultados-micro",
                            "/api/calidad/evaluaciones/*/micro/pdf"
                    ).hasAnyAuthority(
                            RolUsuario.ROL_MICROBIOLOGO.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/calidad/**",
                            "/api/calidad/evaluaciones/archivo/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name(),
                            RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                            RolUsuario.ROL_MICROBIOLOGO.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name()
                    );

                    auth.requestMatchers("/api/produccion/calidad/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                            RolUsuario.ROL_MICROBIOLOGO.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/produccion/ordenes/*/insumos").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_LIDER_ALIMENTOS.name(),
                            RolUsuario.ROL_LIDER_HOMEOPATICOS.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    // Acceso de solo lectura a órdenes de producción y batch record para rol de calidad
                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/produccion/ordenes",
                            "/api/produccion/ordenes/**",
                            "/api/produccion/batch-record/**"
                    ).hasAnyAuthority(
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_LIDER_ALIMENTOS.name(),
                            RolUsuario.ROL_LIDER_HOMEOPATICOS.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
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

                    auth.requestMatchers("/actuator/metrics/**").hasAnyAuthority(
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

                    auth.requestMatchers("/api/inventario/bitacora", "/api/inventario/bitacora/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    // Mantener esta regla genérica al final
                    auth.requestMatchers("/error").permitAll();
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

        // 1. Orígenes configurados por propiedad (si existen)
        java.util.Set<String> originPatterns = new java.util.LinkedHashSet<>();
        if (allowedOriginsProp != null && !allowedOriginsProp.trim().isEmpty()) {
            originPatterns.addAll(
                    java.util.Arrays.stream(allowedOriginsProp.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty()) // antes: isBlank()
                            .toList()
            );
        }

        // 2. Siempre permitir localhost para trabajo local,
        //    sin importar si la propiedad vino o no.
        originPatterns.add("http://localhost:5173");
        originPatterns.add("http://127.0.0.1:5173");

        // 3. Si hay al menos un origen externo, agregar túnel y Vercel
        boolean hasExternalOrigin = originPatterns.stream().anyMatch(origin ->
                !(origin.contains("://localhost") || origin.contains("://127.") || origin.contains("://0.0.0.0"))
        );
        if (hasExternalOrigin) {
            originPatterns.add("https://*.trycloudflare.com");
            originPatterns.add("https://clemen-integra-demo-front.vercel.app");
        }

        configuration.setAllowedOriginPatterns(new java.util.ArrayList<>(originPatterns));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setExposedHeaders(java.util.List.of("Content-Disposition", "Location"));

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
