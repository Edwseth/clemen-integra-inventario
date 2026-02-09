package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.inventario.controller.AjusteInventarioController;
import com.willyes.clemenintegra.inventario.controller.AlertaInventarioController;
import com.willyes.clemenintegra.inventario.controller.CategoriaProductoController;
import com.willyes.clemenintegra.inventario.controller.ConteoCiclicoController;
import com.willyes.clemenintegra.inventario.controller.LoteProductoController;
import com.willyes.clemenintegra.inventario.controller.MovimientoInventarioController;
import com.willyes.clemenintegra.inventario.controller.ProductoController;
import com.willyes.clemenintegra.inventario.controller.SolicitudMovimientoController;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.AjusteInventarioService;
import com.willyes.clemenintegra.inventario.service.AlertaInventarioService;
import com.willyes.clemenintegra.inventario.service.CategoriaProductoService;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ConteoCiclicoController.class,
        AjusteInventarioController.class,
        SolicitudMovimientoController.class,
        ProductoController.class,
        CategoriaProductoController.class,
        MovimientoInventarioController.class,
        LoteProductoController.class,
        AlertaInventarioController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, ContadorRoleSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class ContadorRoleSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConteoCiclicoService conteoCiclicoService;
    @MockBean
    private AjusteInventarioService ajusteInventarioService;
    @MockBean
    private SolicitudMovimientoService solicitudMovimientoService;
    @MockBean
    private ProductoService productoService;
    @MockBean
    private CategoriaProductoService categoriaProductoService;
    @MockBean
    private MovimientoInventarioService movimientoInventarioService;
    @MockBean
    private LoteProductoService loteProductoService;
    @MockBean
    private AlertaInventarioService alertaInventarioService;
    @MockBean
    private ProductoRepository productoRepository;
    @MockBean
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @MockBean
    private UnidadMedidaRepository unidadMedidaRepository;
    @MockBean
    private LoteProductoRepository loteProductoRepository;
    @MockBean
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @MockBean
    private StockQueryService stockQueryService;
    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;
    @MockBean
    private LoteProductoMapper loteProductoMapper;
    @MockBean
    private EvaluacionCalidadService evaluacionCalidadService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private UsuarioRepository usuarioRepository;

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
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorPuedeListarConteos() throws Exception {
        when(conteoCiclicoService.listar(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/inventario/conteos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeCrearConteo() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeActualizarConteo() throws Exception {
        mockMvc.perform(put("/api/inventario/conteos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeAplicarNiCerrarConteo() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos/1/aplicar"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/inventario/conteos/1/cerrar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorPuedeListarYAjustarInventario() throws Exception {
        when(ajusteInventarioService.listar(any())).thenReturn(new PageImpl<>(List.of()));
        when(ajusteInventarioService.crear(any(AjusteInventarioRequestDTO.class))).thenReturn(
                AjusteInventarioResponseDTO.builder()
                        .id(10L)
                        .fecha(LocalDateTime.now())
                        .motivo("Ajuste")
                        .observaciones("ok")
                        .cantidad(BigDecimal.ONE)
                        .productoNombre("P")
                        .almacenNombre("A")
                        .usuarioNombre("U")
                        .build()
        );

        mockMvc.perform(get("/api/inventario/ajustes"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motivo":"Ajuste",
                                  "observaciones":"ok",
                                  "cantidad":1,
                                  "productoId":1,
                                  "almacenId":1,
                                  "usuarioId":1
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorPuedeLeerModulosInventarioPermitidos() throws Exception {
        when(productoService.listarTodos(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(categoriaProductoService.listarTodas()).thenReturn(List.of());
        when(movimientoInventarioService.filtrar(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(loteProductoService.listarTodos(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(alertaInventarioService.obtenerAlertasInventario(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/movimientos/filtrar")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lotes").param("vencidos", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventario/alertas").param("diasVencimiento", "30"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    void contadorNoPuedeListarSolicitudes() throws Exception {
        mockMvc.perform(get("/api/inventario/solicitudes"))
                .andExpect(status().isForbidden());
    }
}
