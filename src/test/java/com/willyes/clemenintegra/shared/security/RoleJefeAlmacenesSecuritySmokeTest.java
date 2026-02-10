package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.bom.controller.FormulaProductoController;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.service.FormulaProductoService;
import com.willyes.clemenintegra.calidad.controller.NoConformidadController;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import com.willyes.clemenintegra.documental.controller.ControlDocumentalController;
import com.willyes.clemenintegra.documental.service.ControlDocumentalService;
import com.willyes.clemenintegra.inventario.controller.AjusteInventarioController;
import com.willyes.clemenintegra.inventario.controller.OrdenCompraController;
import com.willyes.clemenintegra.inventario.controller.ProductoController;
import com.willyes.clemenintegra.inventario.controller.SolicitudMovimientoController;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.*;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.service.UsuarioService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ProductoController.class,
        AjusteInventarioController.class,
        SolicitudMovimientoController.class,
        OrdenCompraController.class,
        ControlDocumentalController.class,
        NoConformidadController.class,
        FormulaProductoController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, RoleJefeAlmacenesSecuritySmokeTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class RoleJefeAlmacenesSecuritySmokeTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ProductoService productoService;
    @MockBean private ProductoRepository productoRepository;
    @MockBean private MovimientoInventarioRepository movimientoInventarioRepository;
    @MockBean private UnidadMedidaRepository unidadMedidaRepository;

    @MockBean private AjusteInventarioService ajusteInventarioService;

    @MockBean private SolicitudMovimientoService solicitudMovimientoService;
    @MockBean private UsuarioService usuarioService;

    @MockBean private OrdenCompraRepository ordenCompraRepository;
    @MockBean private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @MockBean private ProveedorRepository proveedorRepository;
    @MockBean private OrdenCompraService ordenCompraService;
    @MockBean private HistorialEstadoOrdenService historialEstadoOrdenService;
    @MockBean private RecepcionOCService recepcionOCService;
    @MockBean private OrdenCompraPdfService ordenCompraPdfService;
    @MockBean private com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper ordenCompraMapper;

    @MockBean private ControlDocumentalService controlDocumentalService;

    @MockBean private NoConformidadService noConformidadService;

    @MockBean private FormulaProductoService formulaProductoService;
    @MockBean private BomMapper bomMapper;

    @MockBean private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean private RequestTimingFilter requestTimingFilter;
    @MockBean private RequestIdFilter requestIdFilter;
    @MockBean private SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;
    @MockBean private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() throws ServletException, IOException {
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

        when(productoService.listarTodos(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        when(productoService.crearProducto(any(), anyLong())).thenReturn(ProductoResponseDTO.builder().id(1L).build());
        when(ajusteInventarioService.listar(any())).thenReturn(Page.empty());

        Usuario usuario = Usuario.builder()
                .id(10L)
                .nombreUsuario("jefe")
                .correo("jefe@demo.com")
                .nombreCompleto("Jefe")
                .clave("x")
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .activo(true)
                .bloqueado(false)
                .build();
        when(usuarioService.buscarPorNombreUsuario(anyString())).thenReturn(usuario);
        when(solicitudMovimientoService.aprobarSolicitud(anyLong(), anyLong())).thenReturn(null);

        when(ordenCompraService.listar(any(), any(Boolean.class))).thenReturn(new PageImpl<>(List.of()));
        when(controlDocumentalService.buscarDocumentos(any(), any(), any(), any(), any())).thenReturn(Page.empty());
    }

    @Test
    void inventarioProductosGetPermitido() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .with(authentication(jefeAuth())))
                .andExpect(status().isOk());
    }

    @Test
    void inventarioProductosPostPermitido() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .with(authentication(jefeAuth()))
                        .contentType("application/json")
                        .content("""
                                {
                                  "sku":"INS-100",
                                  "nombre":"Insumo Test",
                                  "descripcionProducto":"Demo",
                                  "stockMinimo":1,
                                  "unidadMedidaId":1,
                                  "categoriaProductoId":1
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void inventarioAjustesGetPermitidoYEscrituraDenegada() throws Exception {
        mockMvc.perform(get("/api/inventario/ajustes")
                        .with(authentication(jefeAuth())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventario/ajustes")
                        .with(authentication(jefeAuth()))
                        .contentType("application/json")
                        .content("{"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/inventario/ajustes/1")
                        .with(authentication(jefeAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void inventarioSolicitudesAprobarPermitido() throws Exception {
        mockMvc.perform(put("/api/inventario/solicitudes/1/aprobar")
                        .with(authentication(jefeAuth())))
                .andExpect(status().isOk());
    }

    @Test
    void comprasGetPermitidoYPostDenegado() throws Exception {
        mockMvc.perform(get("/api/ordenes-compra")
                        .with(authentication(jefeAuth())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ordenes-compra")
                        .with(authentication(jefeAuth()))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void calidadGetControlDocumentalPermitidoYEscrituraDenegada() throws Exception {
        mockMvc.perform(get("/api/documental/documentos")
                        .with(authentication(jefeAuth())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/calidad/no-conformidades")
                        .with(authentication(jefeAuth()))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/calidad/no-conformidades/1")
                        .with(authentication(jefeAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void bomGetFormulasDenegado() throws Exception {
        mockMvc.perform(get("/api/bom/formulas")
                        .with(authentication(jefeAuth())))
                .andExpect(status().isForbidden());
    }

    private Authentication jefeAuth() {
        Usuario usuario = Usuario.builder()
                .id(10L)
                .nombreUsuario("jefe")
                .correo("jefe@demo.com")
                .nombreCompleto("Jefe")
                .clave("x")
                .rol(RolUsuario.ROL_JEFE_ALMACENES)
                .activo(true)
                .bloqueado(false)
                .build();
        CustomUserDetails principal = new CustomUserDetails(
                usuario,
                List.of(new SimpleGrantedAuthority(RolUsuario.ROL_JEFE_ALMACENES.name())));
        return new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                principal.getAuthorities());
    }
}
