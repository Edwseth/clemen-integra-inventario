package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadCreateRequest;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDownloadDTO;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadEstado;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import com.willyes.clemenintegra.calidad.service.DocumentoCalidadService;
import com.willyes.clemenintegra.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import com.willyes.clemenintegra.support.TestMethodSecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentoCalidadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestMethodSecurityConfig.class, GlobalExceptionHandler.class})
class DocumentoCalidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentoCalidadService service;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @WithMockUser(authorities = "QC_READ")
    void listarDocumentosDevuelvePagina() throws Exception {
        DocumentoCalidadDTO dto = DocumentoCalidadDTO.builder()
                .id(1L)
                .tipo(DocumentoCalidadTipo.SANITIZACION)
                .nombre("Procedimiento")
                .estado(DocumentoCalidadEstado.VIGENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(service.listar(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/calidad/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @WithMockUser(authorities = "QC_WRITE")
    void crearDocumentoRetornaOk() throws Exception {
        DocumentoCalidadDTO respuesta = DocumentoCalidadDTO.builder()
                .id(9L)
                .tipo(DocumentoCalidadTipo.CALIBRACION)
                .nombre("Plan de calibración")
                .estado(DocumentoCalidadEstado.VIGENTE)
                .build();
        when(service.crear(any(DocumentoCalidadCreateRequest.class), any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/calidad/documentos")
                        .contentType(APPLICATION_JSON)
                        .content("{\"tipo\":\"CALIBRACION\",\"nombre\":\"Plan de calibración\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    @WithMockUser(authorities = "QC_WRITE")
    void subirVersionRetornaOk() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "doc.pdf",
                "application/pdf",
                "contenido".getBytes()
        );
        DocumentoCalidadVersionDTO version = DocumentoCalidadVersionDTO.builder()
                .id(7L)
                .documentoId(3L)
                .version(1)
                .nombreArchivo("doc.pdf")
                .contentType("application/pdf")
                .build();
        when(service.subirVersion(eq(3L), any(), any(), any())).thenReturn(version);

        mockMvc.perform(multipart("/api/calidad/documentos/3/versiones")
                        .file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @WithMockUser(authorities = "QC_EXPORT")
    void descargarVersionRetornaArchivo() throws Exception {
        DocumentoCalidadVersionDownloadDTO descarga = DocumentoCalidadVersionDownloadDTO.builder()
                .nombreArchivo("manual.pdf")
                .contentType("application/pdf")
                .contenido("pdf".getBytes())
                .build();
        when(service.descargarVersion(4L)).thenReturn(descarga);

        mockMvc.perform(get("/api/calidad/documentos/versiones/4/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"manual.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

}
