package com.willyes.clemenintegra.bom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaRequest;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponse;
import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.TipoDocumento;
import com.willyes.clemenintegra.bom.service.DocumentoFormulaService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.CustomUserDetailsService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentoFormulaController.class)
@Import(SecurityConfig.class)
class DocumentoFormulaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentoFormulaService documentoService;
    @MockBean
    private ProductoService productoService;

    // Security beans required by SecurityConfig
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        Mockito.reset(documentoService, productoService);
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_PRODUCCION")
    void registrarDocumentoValido_retornaOk() throws Exception {
        DocumentoFormulaRequest request = DocumentoFormulaRequest.builder()
                .formulaId(1L)
                .tipoDocumento("MSDS")
                .rutaArchivo("/docs/msds.pdf")
                .build();

        DocumentoFormula saved = DocumentoFormula.builder()
                .id(10L)
                .tipoDocumento(TipoDocumento.MSDS)
                .rutaArchivo("/docs/msds.pdf")
                .formula(FormulaProducto.builder().id(1L).build())
                .build();

        when(documentoService.guardar(any())).thenReturn(saved);

        mockMvc.perform(post("/api/bom/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.tipoDocumento").value("MSDS"))
                .andExpect(jsonPath("$.rutaArchivo").value("/docs/msds.pdf"));
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_PRODUCCION")
    void registrarDocumento_formulaInexistente_retornaBadRequest() throws Exception {
        DocumentoFormulaRequest request = DocumentoFormulaRequest.builder()
                .formulaId(99L)
                .tipoDocumento("MSDS")
                .rutaArchivo("/docs/msds.pdf")
                .build();

        when(documentoService.guardar(any())).thenThrow(new IllegalArgumentException("Formula no encontrada"));

        mockMvc.perform(post("/api/bom/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Formula no encontrada"));
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_PRODUCCION")
    void obtenerDocumentoPorId_existente_retornaOk() throws Exception {
        DocumentoFormula entidad = DocumentoFormula.builder()
                .id(5L)
                .tipoDocumento(TipoDocumento.INSTRUCTIVO)
                .rutaArchivo("/docs/instr.pdf")
                .formula(FormulaProducto.builder().id(1L).build())
                .build();

        when(documentoService.buscarPorId(5L)).thenReturn(Optional.of(entidad));

        mockMvc.perform(get("/api/bom/documentos/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.tipoDocumento").value("INSTRUCTIVO"));
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_PRODUCCION")
    void obtenerDocumentoPorId_inexistente_retornaNotFound() throws Exception {
        when(documentoService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/bom/documentos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_PRODUCCION")
    void listarDocumentos_retornaLista() throws Exception {
        DocumentoFormula entidad = DocumentoFormula.builder()
                .id(3L)
                .tipoDocumento(TipoDocumento.PROCEDIMIENTO)
                .rutaArchivo("/docs/proc.pdf")
                .formula(FormulaProducto.builder().id(1L).build())
                .build();
        when(documentoService.listarTodas()).thenReturn(List.of(entidad));

        mockMvc.perform(get("/api/bom/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3L));
    }
}
