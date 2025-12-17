package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoDescargaDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoResponseDTO;
import com.willyes.clemenintegra.inventario.service.OrdenCompraDocumentoService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrdenCompraDocumentoController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class OrdenCompraDocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdenCompraDocumentoService documentoService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private UsuarioRepository usuarioRepository;

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
    }

    @Test
    @DisplayName("POST /api/ordenes-compra/{id}/documentos con rol comprador retorna 200")
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void subirDocumentos_ok() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivos", "cert.pdf", "application/pdf", "data".getBytes());

        when(documentoService.subirDocumentos(anyLong(), any(), any(), anyLong()))
                .thenReturn(List.of(
                        OrdenCompraDocumentoResponseDTO.builder()
                                .id(1L)
                                .nombreVisible("Cert")
                                .fechaCreacion(LocalDateTime.now())
                                .build()));

        mockMvc.perform(multipart("/api/ordenes-compra/{ocId}/documentos", 5L)
                        .file(archivo)
                        .param("documentosAdjuntos[0].nombreVisible", "Cert")
                        .param("documentosAdjuntos[0].tipoDocumento", "CERTIFICADO"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/ordenes-compra/{id}/documentos listado con rol comprador")
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void listar_ok() throws Exception {
        when(documentoService.listar(anyLong()))
                .thenReturn(List.of(OrdenCompraDocumentoResponseDTO.builder()
                        .id(2L)
                        .nombreVisible("Factura")
                        .build()));

        mockMvc.perform(get("/api/ordenes-compra/{ocId}/documentos", 7L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/ordenes-compra/documentos/{id}/download responde attachment")
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void descargar_ok() throws Exception {
        when(documentoService.descargar(anyLong()))
                .thenReturn(new OrdenCompraDocumentoDescargaDTO(new ByteArrayResource("bytes".getBytes()),
                        "guia.pdf",
                        "application/pdf"));

        mockMvc.perform(get("/api/ordenes-compra/documentos/{docId}/download", 9L))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guia.pdf\""));
    }
}
