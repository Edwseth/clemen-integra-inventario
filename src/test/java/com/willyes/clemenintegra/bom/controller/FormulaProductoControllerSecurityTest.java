package com.willyes.clemenintegra.bom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.bom.dto.CambiarEstadoFormulaRequest;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaDescargaDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponseDTO;
import com.willyes.clemenintegra.bom.dto.FormulaActivaProduccionDTO;
import com.willyes.clemenintegra.bom.dto.FormulaProductoSelectorDTO;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.service.DocumentoFormulaService;
import com.willyes.clemenintegra.bom.service.FormulaProductoService;
import com.willyes.clemenintegra.inventario.repository.UnidadMedidaRepository;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({FormulaProductoController.class, DocumentoFormulaController.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
@Import(SecurityConfig.class)
class FormulaProductoControllerSecurityTest {

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
    private DocumentoFormulaService documentoFormulaService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private UsuarioRepository usuarioRepository;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails jefeProduccionDetails;
    private CustomUserDetails jefeCalidadDetails;
    private CustomUserDetails superAdminDetails;

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

        FormulaProductoSelectorDTO resumenDTO = new FormulaProductoSelectorDTO(
                1L,
                "v1",
                EstadoFormula.BORRADOR,
                10L,
                "P-001",
                "Producto Test");

        FormulaActivaProduccionDTO formulaActiva = new FormulaActivaProduccionDTO();
        formulaActiva.formulaId = 2L;
        formulaActiva.productoId = 10L;
        formulaActiva.codigoProducto = "P-001";
        formulaActiva.nombreProducto = "Producto Test";
        formulaActiva.version = "v1";
        formulaActiva.estado = EstadoFormula.APROBADA;
        formulaActiva.activo = true;

        DocumentoFormulaResponseDTO documentoResponse = DocumentoFormulaResponseDTO.builder()
                .id(5L)
                .formulaId(1L)
                .tipoDocumento("PROCEDIMIENTO")
                .nombreArchivo("archivo.pdf")
                .nombreVisible("Archivo")
                .rutaArchivo("/tmp/archivo.pdf")
                .build();

        FormulaProducto formula = new FormulaProducto();
        formula.setId(3L);

        when(formulaProductoService.listarResumen(any(), any(), any())).thenReturn(new PageImpl<>(List.of(resumenDTO)));
        when(formulaProductoService.obtenerFormulaActivaProduccion(anyLong())).thenReturn(formulaActiva);
        when(formulaProductoService.clonarFormula(anyLong(), anyLong())).thenReturn(formula);
        when(formulaProductoService.cambiarEstado(anyLong(), any(EstadoFormula.class), anyLong())).thenReturn(formula);
        when(documentoFormulaService.listarDocumentos(anyLong())).thenReturn(List.of(documentoResponse));
        when(documentoFormulaService.guardarDocumento(anyLong(), any(), any(), any()))
                .thenReturn(documentoResponse);
        when(documentoFormulaService.descargarDocumento(anyLong()))
                .thenReturn(new DocumentoFormulaDescargaDTO(new ByteArrayResource("data".getBytes()), "archivo.pdf", MediaType.APPLICATION_PDF_VALUE));

        jefeProduccionDetails = buildUserDetails(10L, RolUsuario.ROL_JEFE_PRODUCCION);
        jefeCalidadDetails = buildUserDetails(11L, RolUsuario.ROL_JEFE_CALIDAD);
        superAdminDetails = buildUserDetails(12L, RolUsuario.ROL_SUPER_ADMIN);
    }

    @Test
    @DisplayName("GET /api/bom/formulas sin autenticación retorna 401")
    void listarFormulas_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/bom/formulas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    @DisplayName("Accesos BOM con rol no autorizado responden 403")
    void accesosConRolNoAutorizado_retorna403() throws Exception {
        mockMvc.perform(get("/api/bom/formulas"))
                .andExpect(status().isForbidden());

        CambiarEstadoFormulaRequest request = new CambiarEstadoFormulaRequest(EstadoFormula.APROBADA);
        mockMvc.perform(post("/api/bom/formulas/{id}/cambiar-estado", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("ROL_JEFE_PRODUCCION solo puede realizar operaciones de lectura en BOM")
    void jefeProduccionSoloLectura() throws Exception {
        mockMvc.perform(get("/api/bom/formulas"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/{formulaId}/documentos", 1L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/documentos/{documentoId}/descargar", 5L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/producto/{productoId}/formula-activa", 10L))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bom/formulas/{id}/clonar", 1L)
                        .with(user(jefeProduccionDetails)))
                .andExpect(status().isForbidden());

        CambiarEstadoFormulaRequest request = new CambiarEstadoFormulaRequest(EstadoFormula.APROBADA);
        mockMvc.perform(post("/api/bom/formulas/{id}/cambiar-estado", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(jefeProduccionDetails)))
                .andExpect(status().isForbidden());

        MockMultipartFile archivo = new MockMultipartFile("archivo", "nota.txt", MediaType.TEXT_PLAIN_VALUE, "demo".getBytes());
        mockMvc.perform(multipart("/api/bom/formulas/{id}/documentos", 1L).file(archivo)
                        .with(user(jefeProduccionDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/bom/formulas/documentos/{documentoId}", 5L)
                        .with(user(jefeProduccionDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("ROL_JEFE_CALIDAD tiene acceso completo a BOM")
    void jefeCalidadAccesoCompleto() throws Exception {
        mockMvc.perform(get("/api/bom/formulas"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/{formulaId}/documentos", 1L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/documentos/{documentoId}/descargar", 5L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/producto/{productoId}/formula-activa", 10L))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bom/formulas/{id}/clonar", 1L)
                        .with(user(jefeCalidadDetails)))
                .andExpect(status().isCreated());

        CambiarEstadoFormulaRequest request = new CambiarEstadoFormulaRequest(EstadoFormula.APROBADA);
        mockMvc.perform(post("/api/bom/formulas/{id}/cambiar-estado", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(jefeCalidadDetails)))
                .andExpect(status().isOk());

        MockMultipartFile archivo = new MockMultipartFile("archivo", "nota.txt", MediaType.TEXT_PLAIN_VALUE, "demo".getBytes());
        mockMvc.perform(multipart("/api/bom/formulas/{id}/documentos", 1L).file(archivo)
                        .with(user(jefeCalidadDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/bom/formulas/documentos/{documentoId}", 5L)
                        .with(user(jefeCalidadDetails)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    @DisplayName("ROL_SUPER_ADMIN puede realizar todas las operaciones BOM")
    void superAdminAccesoCompleto() throws Exception {
        mockMvc.perform(get("/api/bom/formulas"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/{formulaId}/documentos", 1L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/documentos/{documentoId}/descargar", 5L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bom/formulas/producto/{productoId}/formula-activa", 10L))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bom/formulas/{id}/clonar", 1L)
                        .with(user(superAdminDetails)))
                .andExpect(status().isCreated());

        CambiarEstadoFormulaRequest request = new CambiarEstadoFormulaRequest(EstadoFormula.APROBADA);
        mockMvc.perform(post("/api/bom/formulas/{id}/cambiar-estado", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(superAdminDetails)))
                .andExpect(status().isOk());

        MockMultipartFile archivo = new MockMultipartFile("archivo", "nota.txt", MediaType.TEXT_PLAIN_VALUE, "demo".getBytes());
        mockMvc.perform(multipart("/api/bom/formulas/{id}/documentos", 1L).file(archivo)
                        .with(user(superAdminDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/bom/formulas/documentos/{documentoId}", 5L)
                        .with(user(superAdminDetails)))
                .andExpect(status().isNoContent());
    }

    private CustomUserDetails buildUserDetails(long id, RolUsuario rol) {
        Usuario usuario = Usuario.builder()
                .id(id)
                .nombreUsuario("usuario" + id)
                .nombreCompleto("Usuario " + id)
                .correo("usuario" + id + "@test.com")
                .clave("clave")
                .rol(rol)
                .activo(true)
                .bloqueado(false)
                .build();
        return new CustomUserDetails(usuario);
    }
}
