package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.documental.controller.ControlDocumentalController;
import com.willyes.clemenintegra.documental.dto.DocumentoDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDownloadDTO;
import com.willyes.clemenintegra.documental.service.ControlDocumentalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControlDocumentalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ControlDocumentalDocPermissionsSmokeTest.MethodSecurityConfig.class)
class ControlDocumentalDocPermissionsSmokeTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ControlDocumentalService controlDocumentalService;

    @Test
    @WithMockUser(authorities = "DOC_READ")
    void conDocReadPuedeListarPeroNoSubirDescargarEliminar() throws Exception {
        when(controlDocumentalService.buscarDocumentos(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/documental/documentos"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/documental/documentos")
                        .contentType("application/json")
                        .content("{\"codigo\":\"DOC-1\",\"nombre\":\"Manual\",\"tipo\":\"PROCEDIMIENTO\",\"area\":\"CALIDAD\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/documental/documentos/1/versiones/1/archivo"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/documental/documentos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DOC_WRITE")
    void conDocWritePuedeSubirPeroNoDescargarNiEliminar() throws Exception {
        when(controlDocumentalService.crearDocumento(any(), any())).thenReturn(DocumentoDTO.builder().build());
        when(controlDocumentalService.agregarVersion(anyLong(), any(), any(MultipartFile.class), any()))
                .thenReturn(DocumentoVersionDTO.builder().build());

        mockMvc.perform(post("/api/documental/documentos")
                        .contentType("application/json")
                        .content("{\"codigo\":\"DOC-2\",\"nombre\":\"Instructivo\",\"tipo\":\"PROCEDIMIENTO\",\"area\":\"CALIDAD\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/documental/documentos/1/versiones")
                        .file("archivo", "ok".getBytes()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documental/documentos/1/versiones/1/archivo"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/documental/documentos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DOC_EXPORT")
    void conDocExportPuedeDescargar() throws Exception {
        when(controlDocumentalService.descargarArchivoVersion(1L, 1L))
                .thenReturn(new DocumentoVersionDownloadDTO(new ByteArrayResource("ok".getBytes()), "doc.txt", "text/plain"));

        mockMvc.perform(get("/api/documental/documentos/1/versiones/1/archivo"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "DOC_DELETE")
    void conDocDeletePuedeEliminar() throws Exception {
        doNothing().when(controlDocumentalService).eliminarDocumento(1L);

        mockMvc.perform(delete("/api/documental/documentos/1"))
                .andExpect(status().isNoContent());
    }
}
