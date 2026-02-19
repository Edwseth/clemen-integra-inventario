package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.CapaArchivoDescargaDTO;
import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.service.CapaService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import java.util.List;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.willyes.clemenintegra.support.TestAuth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CapaController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(com.willyes.clemenintegra.shared.security.SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class CapaControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CapaService capaService;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.repository.UsuarioRepository usuarioRepository;


    @Test
    void permiteGetConPermisoQcRead() throws Exception {
        Page<CapaDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(capaService.listar(null, null, PageRequest.of(0, 20))).thenReturn(page);

        mockMvc.perform(get("/api/calidad/capas?page=0&size=20")
                        .with(TestAuth.auth("analista", "QC_READ"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaGetSinPermisoQcRead() throws Exception {
        mockMvc.perform(get("/api/calidad/capas")
                        .with(TestAuth.auth("sin-qc", "DOC_READ"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaPostSinPermisoQcWriteAunqueTengaQcRead() throws Exception {
        mockMvc.perform(post("/api/calidad/capas")
                        .with(TestAuth.auth("lector", "QC_READ", "DOC_READ"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteCerrarConPermisoCanonicoQcWorkflowFinish() throws Exception {
        when(capaService.cerrar(5L)).thenReturn(new CapaDTO());

        mockMvc.perform(patch("/api/calidad/capas/5/cerrar")
                        .with(TestAuth.auth("finisher", "QC_WORKFLOW_FINISH")))
                .andExpect(status().isOk());
    }

    @Test
    void permiteDescargarArchivoConPermisoCanonicoQcExport() throws Exception {
        when(capaService.descargarArchivo(7L, 9L)).thenReturn(CapaArchivoDescargaDTO.builder()
                .nombreArchivo("evidencia.txt")
                .contentType(MediaType.TEXT_PLAIN_VALUE)
                .contenido("ok".getBytes())
                .build());

        mockMvc.perform(get("/api/calidad/capas/7/archivos/9/descargar")
                        .with(TestAuth.auth("exporter", "QC_EXPORT")))
                .andExpect(status().isOk());
    }
}
