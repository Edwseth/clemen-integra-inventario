package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.inventario.controller.ProductoController;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.planeacion.controller.MrpController;
import com.willyes.clemenintegra.planeacion.controller.PlanProduccionController;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.service.MrpReporteService;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ProductoController.class,
        PlanProduccionController.class,
        MrpController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, PlaneadorRoleSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class PlaneadorRoleSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductoService productoService;
    @MockBean
    private ProductoRepository productoRepository;
    @MockBean
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @MockBean
    private UnidadMedidaRepository unidadMedidaRepository;
    @MockBean
    private UsuarioRepository usuarioRepository;
    @MockBean
    private PlanProduccionService planProduccionService;
    @MockBean
    private MrpService mrpService;
    @MockBean
    private MrpReporteService mrpReporteService;
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
    @WithMockUser(authorities = "ROL_PLANEADOR")
    void planeadorNoPuedeBuscarProductosParaAjustes() throws Exception {
        mockMvc.perform(get("/api/productos/buscar")
                        .param("query", "pro")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROL_INSUFICIENTE"));
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    void planeadorNoPuedeCrearProducto() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "PT-001",
                                  "nombre": "Producto de prueba",
                                  "stockMinimo": 1,
                                  "unidadMedidaId": 1,
                                  "categoriaProductoId": 1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    void planeadorPuedeCrearPlanSemanal() throws Exception {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(1L)
                .semanaInicio(LocalDate.of(2024, 1, 1))
                .semanaFin(LocalDate.of(2024, 1, 7))
                .detalles(new ArrayList<>())
                .build();
        when(planProduccionService.crearOActualizar(any())).thenReturn(plan);

        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "semanaInicio": "2024-01-01",
                                  "semanaFin": "2024-01-07",
                                  "detalles": []
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    void planeadorNoPuedeEjecutarMrp() throws Exception {
        mockMvc.perform(post("/api/mrp/corridas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planSemanalId\": 1}"))
                .andExpect(status().isForbidden());
    }
}
