package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.bom.controller.FormulaProductoController;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.service.FormulaProductoService;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FormulaProductoController.class)
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
@Import(SecurityConfig.class)
class BomCanonicalPermissionsSecuritySmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FormulaProductoService formulaProductoService;
    @MockBean
    private BomMapper bomMapper;
    @MockBean
    private ProductoService productoService;
    @MockBean
    private UnidadMedidaRepository unidadMedidaRepository;

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
    @WithMockUser(authorities = "BOM_READ")
    void bomReadPermiteListarFormulas() throws Exception {
        when(formulaProductoService.listarResumen(any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/bom/formulas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "BOM_READ")
    void sinBomWriteSeRechazaRegistroFormula() throws Exception {
        mockMvc.perform(multipart("/api/bom/formulas")
                        .file("formula", "{}".getBytes()))
                .andExpect(status().isForbidden());
    }
}
