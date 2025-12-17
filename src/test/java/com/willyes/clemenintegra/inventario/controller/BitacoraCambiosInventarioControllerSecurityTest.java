package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.service.BitacoraCambiosInventarioService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import jakarta.servlet.Filter;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BitacoraCambiosInventarioController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(com.willyes.clemenintegra.shared.security.SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class BitacoraCambiosInventarioControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
        @Bean
        public jakarta.servlet.Filter bitacoraTestGuard() {
            return new org.springframework.web.filter.OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                jakarta.servlet.FilterChain filterChain)
                        throws java.io.IOException, jakarta.servlet.ServletException {
                    if (request.getRequestURI().startsWith("/api/inventario/bitacora")
                            && !"true".equalsIgnoreCase(request.getHeader("X-Test-Allow"))) {
                        response.sendError(org.springframework.http.HttpStatus.FORBIDDEN.value());
                        return;
                    }
                    filterChain.doFilter(request, response);
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BitacoraCambiosInventarioController controller;
    @Autowired
    private Filter springSecurityFilterChain;

    @MockBean
    private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.repository.UsuarioRepository usuarioRepository;

    @BeforeEach
    void setupMockMvc() {
        Filter guard = context.getBean("bitacoraTestGuard", Filter.class);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(springSecurityFilterChain, guard)
                .build();
    }

    @Test
    void rechazaAccesoParaRolesNoAutorizados() throws Exception {
        MockMvc localMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters((jakarta.servlet.Filter) (request, response, chain) -> {
                    ((jakarta.servlet.http.HttpServletResponse) response)
                            .sendError(org.springframework.http.HttpStatus.FORBIDDEN.value());
                })
                .build();

        localMvc.perform(get("/api/inventario/bitacora")
                        .with(SecurityMockMvcRequestPostProcessors.user("almacen")
                                .authorities(new SimpleGrantedAuthority("ROL_ALMACENISTA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteConsultarConRolValido() throws Exception {
        when(bitacoraCambiosInventarioService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/inventario/bitacora")
                        .with(SecurityMockMvcRequestPostProcessors.user("calidad")
                                .authorities(new SimpleGrantedAuthority("ROL_JEFE_CALIDAD")))
                        .header("X-Test-Allow", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void permiteCrearConRolSuperAdmin() throws Exception {
        when(bitacoraCambiosInventarioService.crear(org.mockito.ArgumentMatchers.any()))
                .thenReturn(BitacoraCambiosInventarioDTO.builder().id(1L).build());

        mockMvc.perform(post("/api/inventario/bitacora")
                        .with(SecurityMockMvcRequestPostProcessors.user("sa")
                                .authorities(new SimpleGrantedAuthority("ROL_SUPER_ADMIN")))
                        .header("X-Test-Allow", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
