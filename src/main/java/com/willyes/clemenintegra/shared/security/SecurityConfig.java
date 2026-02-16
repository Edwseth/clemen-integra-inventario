package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.shared.security.model.UsuarioPrincipal;
import com.willyes.clemenintegra.shared.security.service.UsuarioAuthoritiesService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UsuarioInactivoFilter usuarioInactivoFilter;
    private final RequestTimingFilter requestTimingFilter;
    private final RequestIdFilter requestIdFilter;
    private final SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;
    private final ObjectProvider<ApiAuthenticationEntryPoint> apiAuthenticationEntryPointProvider;
    private final ObjectProvider<ObjectMapper> objectMapperProvider;
    private final ObjectProvider<JwtAuthenticationProvider> jwtAuthenticationProviderProvider;

    // Orígenes permitidos por perfil (lista separada por comas)
    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsProp;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       UserDetailsService userDetailsService,
                                                       ObjectProvider<PasswordEncoder> passwordEncoderProvider) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        JwtAuthenticationProvider jwtAuthenticationProvider = jwtAuthenticationProviderProvider.getIfAvailable();
        if (jwtAuthenticationProvider != null) {
            builder.authenticationProvider(jwtAuthenticationProvider);
        } else {
            log.warn("SecurityConfig: JwtAuthenticationProvider no está disponible, se usará AuthenticationManager sin el provider personalizado");
        }

        PasswordEncoder passwordEncoder = passwordEncoderProvider.orderedStream().findFirst().orElse(null);
        if (passwordEncoder != null) {
            builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        } else {
            log.warn("SecurityConfig: PasswordEncoder no está disponible, se registrará UserDetailsService sin codificador");
            builder.userDetailsService(userDetailsService);
        }
        AuthenticationManager authenticationManager = builder.build();
        log.debug("SecurityConfig: AuthenticationManager configurado con UserDetailsService{}{}",
                jwtAuthenticationProvider != null ? " y JwtAuthenticationProvider" : "",
                passwordEncoder != null ? " y PasswordEncoder" : "");
        return authenticationManager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
                                                   AuthenticationManager authenticationManager) throws Exception {
        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    auth.requestMatchers(
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info",
                            "/api/health",
                            "/api/health/**",
                            "/auth/login",
                            "/api/auth/login",
                            "/api/auth/verificar",
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

                    auth.requestMatchers(HttpMethod.PATCH, "/api/inventario/productos/*/calidad").hasAnyAuthority(
                            "INV_WRITE",
                            "INV_DECIDE",
                            "QC_DECIDE"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/inventario/productos/**").hasAnyAuthority(
                            "INV_PRODUCT_READ",
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/categorias", "/api/categorias/**").hasAnyAuthority(
                            "INV_CATEGORIAS_READ",
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_PLANEADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.GET,
                            "/api/produccion/ordenes/alertas",
                            "/api/produccion/ordenes/alertas/**").hasAnyAuthority(
                            "PROD_ALERTAS_READ"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/productos/**").hasAnyAuthority(
                            "INV_PRODUCT_READ",
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_COMPRADOR.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_ANALISTA_CALIDAD.name(),
                            RolUsuario.ROL_MICROBIOLOGO.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_PLANEADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(
                            "/api/productos/**",
                            "/api/motivos/**", "/api/lotes/**", "/api/almacenes/**",
                            "/api/proveedores/**",
                            "/api/inventario/historial-ordenes/**"
                    ).hasAnyAuthority(
                            "INV_READ",
                            "INV_WRITE"
                    );

                    auth.requestMatchers("/api/inventario/ordenes/**").hasAnyAuthority(
                            "INV_READ",
                            "INV_WRITE",
                            "INV_WORKFLOW",
                            "INV_DECIDE",
                            "PO_READ",
                            "PO_WRITE",
                            "PO_WORKFLOW",
                            "PO_DECIDE"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/inventario/conteos/**").hasAnyAuthority(
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name(),
                            "INV_CONTEOS_READ"
                    );

                    auth.requestMatchers(HttpMethod.POST, "/api/inventario/conteos/*/iniciar", "/api/inventario/conteos/*/en-conteo").hasAnyAuthority(
                            "INV_CONTEOS_START"
                    );

                    auth.requestMatchers(HttpMethod.POST, "/api/inventario/conteos/*/aplicar").hasAnyAuthority(
                            "INV_CONTEOS_APPLY"
                    );

                    auth.requestMatchers(HttpMethod.POST, "/api/inventario/conteos/*/cerrar").hasAnyAuthority(
                            "INV_CONTEOS_CLOSE"
                    );

                    auth.requestMatchers("/api/inventario/conteos/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name(),
                            "INV_CONTEOS_WRITE"
                    );


                    auth.requestMatchers(HttpMethod.GET,
                            "/api/inventario/ordenes/**",
                            "/api/ordenes-compra/**").hasAnyAuthority(
                            "INV_READ",
                            "INV_WORKFLOW",
                            "PO_READ"
                    );

                    auth.requestMatchers("/api/ordenes-compra/**").hasAnyAuthority(
                            "INV_WRITE",
                            "INV_WORKFLOW",
                            "INV_DECIDE",
                            "PO_WRITE",
                            "PO_WORKFLOW",
                            "PO_DECIDE"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/inventario/ajustes/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name(),
                            "INV_AJUSTES_READ",
                            "INV_AJUSTES_WRITE"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/movimientos/**").hasAnyAuthority(
                            "INV_MOV_READ",
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_PLANEADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/inventario/alertas/**").hasAnyAuthority(
                            "INV_ALERTAS_READ",
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_PLANEADOR.name(),
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/lotes/**", "/api/inventario/lotes/**").hasAnyAuthority(
                            "INV_LOTES_READ",
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_LIDER_ALIMENTOS.name(),
                            RolUsuario.ROL_LIDER_HOMEOPATICOS.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_PLANEADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/inventario/kardex/**").hasAnyAuthority(
                            "INV_KARDEX_READ",
                            RolUsuario.ROL_JEFE_ALMACENES.name(),
                            RolUsuario.ROL_ALMACENISTA.name(),
                            RolUsuario.ROL_JEFE_PRODUCCION.name(),
                            RolUsuario.ROL_LIDER_ALIMENTOS.name(),
                            RolUsuario.ROL_LIDER_HOMEOPATICOS.name(),
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_PLANEADOR.name(),
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers("/api/movimientos/**", "/api/categorias/**",
                            "/api/inventario/alertas/**").hasAnyAuthority(
                            "INV_WRITE"
                    );

                    auth.requestMatchers("/api/recepciones/**").hasAnyAuthority(
                            "INV_WRITE",
                            "INV_WORKFLOW"
                    );

                    // 1) Lectura de listado de planes semanales (auditoría para Contador)
                    auth.requestMatchers(HttpMethod.GET, "/api/planeacion/planes-semanales").hasAnyAuthority(
                            "PO_READ",
                            "PO_PLAN_SEMANAL_READ"
                    );
                    // 1.1) Detalle de planes semanales: lectura permitida para Contador
                    auth.requestMatchers(HttpMethod.GET, "/api/planeacion/planes-semanales/**").hasAnyAuthority(
                            "PO_READ",
                            "PO_PLAN_SEMANAL_READ"
                    );
                    // 2) Resto de operaciones de planeación (crear/editar/eliminar):
                    auth.requestMatchers("/api/planeacion/**").hasAnyAuthority(
                            "PO_WRITE",
                            "PO_WORKFLOW",
                            "PO_PLAN_SEMANAL_WRITE"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/mrp/**").hasAnyAuthority(
                            "PO_MRP_READ",
                            "PO_READ"
                    );

                    auth.requestMatchers("/api/mrp/**").hasAnyAuthority(
                            "PO_MRP_WRITE",
                            "PO_WRITE"
                    );

                    auth.requestMatchers(HttpMethod.POST, "/api/ordenes-compra/*/documentos").hasAnyAuthority(
                            "INV_WRITE",
                            "PO_WRITE"
                    );

                    auth.requestMatchers(HttpMethod.GET,
                            "/api/ordenes-compra/*/documentos",
                            "/api/ordenes-compra/documentos/**").hasAnyAuthority(
                            "INV_READ",
                            "PO_READ"
                    );


                    auth.requestMatchers(HttpMethod.POST,
                            "/api/documental/documentos",
                            "/api/documental/documentos/*/versiones").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name(),
                            "CONTROL_DOCUMENTAL_WRITE"
                    );

                    auth.requestMatchers(HttpMethod.DELETE,
                            "/api/documental/documentos/**").hasAnyAuthority(
                            "DOC_DELETE"
                    );

                    auth.requestMatchers("/api/documental/documentos/**").authenticated();

                    auth.requestMatchers(HttpMethod.GET, "/api/calidad/capas", "/api/calidad/capas/**").hasAnyAuthority(
                            RolUsuario.ROL_JEFE_CALIDAD.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name(),
                            "CONTROL_DOCUMENTAL_WRITE"
                    );
                    auth.requestMatchers("/api/calidad/capas", "/api/calidad/capas/**").hasAnyAuthority(
                            "QC_WRITE"
                    );

                    auth.requestMatchers(
                            "/api/calidad/plantillas-micro/**",
                            "/api/calidad/evaluaciones/plantillas/micro/**",
                            "/api/calidad/evaluaciones/*/resultados-micro",
                            "/api/calidad/evaluaciones/*/micro/pdf"
                    ).hasAnyAuthority(
                            "QC_READ",
                            "QC_WRITE",
                            "QC_WORKFLOW",
                            "QC_WORKFLOW_FINISH",
                            "QC_DECIDE"
                    );

                    auth.requestMatchers(HttpMethod.GET,
                            "/api/calidad/alertas/**",
                            "/api/calidad/vida-util/**").hasAnyAuthority(
                            "QC_ALERTAS_READ",
                            "QC_READ"
                    );

                    auth.requestMatchers(HttpMethod.GET,
                            "/api/calidad/auditoria-lote/*",
                            "/api/calidad/lotes/*/estado-calidad").hasAnyAuthority(
                            "QC_READ",
                            "QC_WORKFLOW"
                    );

                    auth.requestMatchers(HttpMethod.GET,
                            "/api/calidad/retenciones/**",
                            "/api/calidad/no-conformidades/**").hasAnyAuthority(
                            "QC_READ"
                    );

                    auth.requestMatchers("/api/calidad/**",
                            "/api/calidad/evaluaciones/archivo/**").hasAnyAuthority(
                            "QC_READ",
                            "QC_WRITE",
                            "QC_WORKFLOW",
                            "QC_WORKFLOW_FINISH",
                            "QC_DECIDE",
                            "QC_EXPORT"
                    );

                    auth.requestMatchers("/api/produccion/calidad/**").hasAnyAuthority(
                            "PROD_READ",
                            "QC_READ",
                            "QC_WRITE"
                    );


                    auth.requestMatchers(HttpMethod.GET,
                            "/api/produccion/indicadores",
                            "/api/produccion/ordenes/alertas",
                            "/api/produccion/indicadores/export/excel").hasAnyAuthority(
                            "PROD_INDICADORES_READ",
                            "PROD_INDICADORES_EXPORT",
                            "PROD_ALERTAS_READ",
                            "PROD_REPORTS_READ"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/produccion/ordenes/*/insumos").hasAnyAuthority(
                            "PROD_READ",
                            "PROD_OP_READ"
                    );

                    // Acceso de solo lectura a órdenes de producción y batch record para rol de calidad
                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/produccion/ordenes",
                            "/api/produccion/ordenes/**",
                            "/api/produccion/batch-record/**"
                    ).hasAnyAuthority(
                            "PROD_READ",
                            "PROD_OP_READ",
                            "PROD_BATCH_RECORD_READ"
                    );

                    auth.requestMatchers("/api/produccion/**").hasAnyAuthority(
                            "PROD_READ",
                            "PROD_WRITE",
                            "PROD_WORKFLOW",
                            "PROD_WORKFLOW_START",
                            "PROD_WORKFLOW_FINISH",
                            "PROD_DECIDE"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/bom/formulas/producto/*/formula-activa").hasAnyAuthority(
                            "BOM_READ",
                            "BOM_FORMULA_READ"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/bom/formulas/activa").hasAnyAuthority(
                            "BOM_READ",
                            "BOM_FORMULA_READ"
                    );

                    auth.requestMatchers("/api/bom/**").hasAnyAuthority(
                            "BOM_READ",
                            "BOM_WRITE",
                            "BOM_WORKFLOW",
                            "BOM_WORKFLOW_FINISH",
                            "BOM_DECIDE"
                    );

                    auth.requestMatchers("/api/inventario/ajustes/**").hasAnyAuthority(
                            RolUsuario.ROL_CONTADOR.name(),
                            RolUsuario.ROL_SUPER_ADMIN.name(),
                            "INV_AJUSTES_WRITE"
                    );

                    auth.requestMatchers("/api/reportes/**").hasAnyAuthority(
                            "INV_REPORTES_EXPORT",
                            "PO_EXPORT",
                            "QC_EXPORT",
                            "PROD_REPORTS_READ",
                            "PROD_EXPORT"
                    );

                    auth.requestMatchers("/actuator/metrics/**").hasAnyAuthority(
                            RolUsuario.ROL_SUPER_ADMIN.name()
                    );

                    auth.requestMatchers(HttpMethod.GET,
                            "/api/inventario/solicitudes/**"
                    ).hasAnyAuthority(
                            "INV_READ"
                    );

                    auth.requestMatchers(
                            "/api/inventario/solicitudes/**"
                    ).hasAnyAuthority(
                            "INV_WRITE",
                            "INV_WORKFLOW"
                    );

                    auth.requestMatchers("/api/inventario/bitacora", "/api/inventario/bitacora/**").hasAnyAuthority(
                            "INV_DECIDE",
                            "INV_WRITE"
                    );

                    auth.requestMatchers(HttpMethod.GET, "/api/admin/rbac/**").hasAnyAuthority(
                            "ADMIN_RBAC_READ",
                            "ADMIN_RBAC_WRITE"
                    );

                    auth.requestMatchers("/api/admin/rbac/**").hasAuthority(
                            "ADMIN_RBAC_WRITE"
                    );

                    // Mantener esta regla genérica al final
                    auth.requestMatchers("/error").permitAll();
                    auth.requestMatchers("/api/**").authenticated();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(resolveAuthenticationEntryPoint()));

        http.addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class);

        JwtAuthenticationProvider jwtAuthenticationProvider = jwtAuthenticationProviderProvider.getIfAvailable();
        if (jwtAuthenticationProvider != null) {
            JwtAuthenticationFilter jwtAuthenticationFilter = jwtAuthenticationFilterProvider.getIfAvailable();
            if (jwtAuthenticationFilter != null) {
                log.debug("SecurityConfig: Registrando JwtAuthenticationFilter y JwtAuthenticationProvider en la cadena de filtros");
                http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                        .addFilterAfter(superAdminSoloLecturaWriteBlockFilter, JwtAuthenticationFilter.class)
                        .addFilterAfter(usuarioInactivoFilter, JwtAuthenticationFilter.class)
                        .addFilterAfter(requestTimingFilter, JwtAuthenticationFilter.class);
            } else {
                log.warn("SecurityConfig: JwtAuthenticationFilter no disponible, la cadena se construirá sin el filtro JWT");
            }
            http.authenticationProvider(jwtAuthenticationProvider);
        }

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoderFallback() {
        log.warn("SecurityConfig: registrando PasswordEncoder delegating por no existir uno definido en el contexto");
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private ApiAuthenticationEntryPoint resolveAuthenticationEntryPoint() {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return apiAuthenticationEntryPointProvider.getIfAvailable(() -> new ApiAuthenticationEntryPoint(objectMapper));
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
        configuration.setAllowedHeaders(java.util.List.of(
                "Authorization",
                "Content-Type",
                "Idempotency-Key",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));
        configuration.setExposedHeaders(java.util.List.of("Content-Disposition", "Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository,
                                                 ObjectProvider<UsuarioAuthoritiesService> usuarioAuthoritiesServiceProvider) {
        return username -> usuarioRepository
                .findByNombreUsuario(username.trim())
                .map(usuario -> {
                    UsuarioAuthoritiesService usuarioAuthoritiesService = usuarioAuthoritiesServiceProvider.getIfAvailable();
                    if (usuarioAuthoritiesService == null) {
                        return new UsuarioPrincipal(usuario,
                                List.of(new SimpleGrantedAuthority(usuario.getRol().name())));
                    }
                    return new UsuarioPrincipal(usuario, usuarioAuthoritiesService.buildAuthorities(usuario));
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
