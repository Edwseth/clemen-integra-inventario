package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.service.BitacoraCambiosInventarioService;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BitacoraCambiosInventarioController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class BitacoraCambiosInventarioControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private RequestTimingFilter requestTimingFilter;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void configureFilters() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(usuarioInactivoFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestTimingFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    void permiteGetConInvRead() throws Exception {
        when(bitacoraCambiosInventarioService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/inventario/bitacora")
                        .with(SecurityMockMvcRequestPostProcessors.user("lector")
                                .authorities(() -> "INV_DECIDE")))
                .andExpect(status().isOk());

        verify(bitacoraCambiosInventarioService).listar();
    }

    @Test
    void rechazaGetSinInvReadAunqueTengaRolLegacy() throws Exception {
        mockMvc.perform(get("/api/inventario/bitacora")
                        .with(SecurityMockMvcRequestPostProcessors.user("legacy")
                                .authorities(() -> "ROL_JEFE_CALIDAD")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permitePostConInvWrite() throws Exception {
        when(bitacoraCambiosInventarioService.crear(any()))
                .thenReturn(BitacoraCambiosInventarioDTO.builder().id(1L).build());

        mockMvc.perform(post("/api/inventario/bitacora")
                        .with(SecurityMockMvcRequestPostProcessors.user("editor")
                                .authorities(() -> "INV_WRITE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaPostSinInvWriteAunqueTengaRolLegacy() throws Exception {
        mockMvc.perform(post("/api/inventario/bitacora")
                        .with(SecurityMockMvcRequestPostProcessors.user("legacy")
                                .authorities(() -> "ROL_SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
