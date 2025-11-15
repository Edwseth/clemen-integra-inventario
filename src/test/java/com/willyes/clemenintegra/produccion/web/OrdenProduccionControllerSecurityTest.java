package com.willyes.clemenintegra.produccion.web;

import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.controller.OrdenProduccionController;
import com.willyes.clemenintegra.produccion.service.OrdenProduccionService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdenProduccionController.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
@Import(SecurityConfig.class)
class OrdenProduccionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdenProduccionService ordenProduccionService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private MovimientoInventarioService movimientoInventarioService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
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
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    @DisplayName("GET /api/produccion/ordenes/{id} sin rol permitido devuelve 403")
    void obtenerOrden_sinPermisos_devuelve403() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/produccion/ordenes/{id} sin autenticación devuelve 401")
    void obtenerOrden_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }
}
