package com.willyes.clemenintegra.bom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.service.*;
import com.willyes.clemenintegra.inventario.service.*;
import com.willyes.clemenintegra.shared.security.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentoFormulaController.class)
//@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class DocumentoFormulaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentoFormulaApplicationService documentoService;

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
        DocumentoFormulaRequestDTO request = DocumentoFormulaRequestDTO.builder()
                .formulaId(1L)
                .tipoDocumento("MSDS")
                .rutaArchivo("/docs/msds.pdf")
                .build();

        DocumentoFormulaResponseDTO response = DocumentoFormulaResponseDTO.builder()
                .id(10L)
                .tipoDocumento("MSDS")
                .rutaArchivo("/docs/msds.pdf")
                .build();

        when(documentoService.guardar(any())).thenReturn(response);

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
        DocumentoFormulaRequestDTO request = DocumentoFormulaRequestDTO.builder()
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
        DocumentoFormulaResponseDTO response = DocumentoFormulaResponseDTO.builder()
                .id(5L)
                .tipoDocumento("INSTRUCTIVO")
                .rutaArchivo("/docs/instr.pdf")
                .build();

        when(documentoService.buscarPorId(5L)).thenReturn(response);

        mockMvc.perform(get("/api/bom/documentos/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.tipoDocumento").value("INSTRUCTIVO"));
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_PRODUCCION")
    void obtenerDocumentoPorId_inexistente_retornaNotFound() throws Exception {
        when(documentoService.buscarPorId(99L)).thenThrow(new EntityNotFoundException("Documento no encontrado"));

        mockMvc.perform(get("/api/bom/documentos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ROL_JEFE_PRODUCCION")
    void listarDocumentos_retornaLista() throws Exception {
        DocumentoFormulaResponseDTO response = DocumentoFormulaResponseDTO.builder()
                .id(3L)
                .tipoDocumento("PROCEDIMIENTO")
                .rutaArchivo("/docs/proc.pdf")
                .build();

        when(documentoService.listarTodas()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/bom/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3L));
    }
}
