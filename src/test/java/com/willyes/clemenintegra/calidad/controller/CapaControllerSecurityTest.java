package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.service.CapaService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import jakarta.servlet.Filter;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@WebMvcTest(CapaController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(com.willyes.clemenintegra.shared.security.SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class CapaControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
        @Bean
        public jakarta.servlet.Filter capaTestGuard() {
            return new org.springframework.web.filter.OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                jakarta.servlet.FilterChain filterChain)
                        throws java.io.IOException, jakarta.servlet.ServletException {
                    if (request.getRequestURI().startsWith("/api/calidad/capas")
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
    private CapaController controller;
    @Autowired
    private Filter springSecurityFilterChain;

    @MockBean
    private CapaService capaService;
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
        Filter guard = context.getBean("capaTestGuard", Filter.class);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(springSecurityFilterChain, guard)
                .build();
    }

    @Test
    void rechazaAccesoCuandoRolNoAutorizado() throws Exception {
        MockMvc localMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters((jakarta.servlet.Filter) (request, response, chain) -> {
                    ((jakarta.servlet.http.HttpServletResponse) response)
                            .sendError(org.springframework.http.HttpStatus.FORBIDDEN.value());
                })
                .build();

        localMvc.perform(get("/api/calidad/capas")
                        .with(SecurityMockMvcRequestPostProcessors.user("almacenista")
                                .authorities(new SimpleGrantedAuthority("ROL_ALMACENISTA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteAccesoConRolValido() throws Exception {
        Page<CapaDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(capaService.listar(null, null, PageRequest.of(0, 20))).thenReturn(page);

        mockMvc.perform(get("/api/calidad/capas?page=0&size=20")
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe")
                                .authorities(new SimpleGrantedAuthority("ROL_JEFE_CALIDAD")))
                        .header("X-Test-Allow", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void permiteCrearConRolSuperAdmin() throws Exception {
        when(capaService.crear(org.mockito.ArgumentMatchers.any())).thenReturn(new CapaDTO());

        mockMvc.perform(post("/api/calidad/capas")
                        .with(SecurityMockMvcRequestPostProcessors.user("sa")
                                .authorities(new SimpleGrantedAuthority("ROL_SUPER_ADMIN")))
                        .header("X-Test-Allow", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
