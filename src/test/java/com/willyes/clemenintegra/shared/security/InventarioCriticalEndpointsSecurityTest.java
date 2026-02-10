package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.inventario.controller.OrdenCompraDetalleController;
import com.willyes.clemenintegra.inventario.controller.TipoMovimientoDetalleController;
import com.willyes.clemenintegra.inventario.controller.UnidadMedidaController;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraDetalleMapper;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.OrdenCompraDetalleService;
import com.willyes.clemenintegra.inventario.service.TipoMovimientoDetalleService;
import com.willyes.clemenintegra.inventario.service.UnidadMedidaService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UnidadMedidaController.class,
        OrdenCompraDetalleController.class,
        TipoMovimientoDetalleController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, InventarioCriticalEndpointsSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class InventarioCriticalEndpointsSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnidadMedidaService unidadMedidaService;
    @MockBean
    private OrdenCompraDetalleService ordenCompraDetalleService;
    @MockBean
    private OrdenCompraRepository ordenCompraRepository;
    @MockBean
    private ProductoRepository productoRepository;
    @MockBean
    private OrdenCompraDetalleMapper ordenCompraDetalleMapper;
    @MockBean
    private TipoMovimientoDetalleService tipoMovimientoDetalleService;

    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private RequestTimingFilter requestTimingFilter;
    @MockBean
    private RequestIdFilter requestIdFilter;
    @MockBean
    private SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;
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

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestIdFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(superAdminSoloLecturaWriteBlockFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeModificarNiEliminarUnidad() throws Exception {
        mockMvc.perform(put("/api/unidades/1")
                        .contentType("application/json")
                        .content("{\"nombre\":\"Kilogramo\",\"simbolo\":\"KG\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/unidades/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    void planeadorNoPuedeEliminarDetallesSensibles() throws Exception {
        mockMvc.perform(delete("/api/inventario/ordenes-compra-detalle/1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/inventario/tipos-movimiento-detalle/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void jefeAlmacenesPuedeEliminarUnidadesYTiposMovimientoDetalle() throws Exception {
        mockMvc.perform(delete("/api/unidades/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/inventario/tipos-movimiento-detalle/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void compradorPuedeEliminarOrdenCompraDetalle() throws Exception {
        mockMvc.perform(delete("/api/inventario/ordenes-compra-detalle/1"))
                .andExpect(status().isNoContent());
    }
}
