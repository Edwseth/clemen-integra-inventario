package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.CapaArchivoDTO;
import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.TipoCapa;
import com.willyes.clemenintegra.calidad.service.CapaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CapaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CapaControllerTest.MethodSecurityTestConfig.class)
class CapaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CapaService capaService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void listarCapasDevuelvePagina() throws Exception {
        CapaDTO capa = CapaDTO.builder()
                .id(1L)
                .noConformidadId(9L)
                .responsableId(3L)
                .tipo(TipoCapa.CORRECTIVA)
                .estado(EstadoCapa.ACTIVA)
                .fechaInicio(LocalDateTime.now())
                .build();
        when(capaService.listar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(capa)));

        mockMvc.perform(get("/api/calidad/capas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void crearCapaRetorna201() throws Exception {
        CapaDTO respuesta = CapaDTO.builder()
                .id(2L)
                .noConformidadId(9L)
                .responsableId(5L)
                .tipo(TipoCapa.CORRECTIVA)
                .estado(EstadoCapa.ACTIVA)
                .fechaInicio(LocalDateTime.now())
                .build();

        when(capaService.crear(any(CapaDTO.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/calidad/capas")
                        .contentType(APPLICATION_JSON)
                        .content("{\"noConformidadId\":9,\"tipo\":\"CORRECTIVA\",\"responsableId\":5,\"fechaInicio\":\"2024-05-01T10:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void cerrarCapaRetornaOk() throws Exception {
        CapaDTO respuesta = CapaDTO.builder()
                .id(3L)
                .noConformidadId(2L)
                .responsableId(7L)
                .tipo(TipoCapa.PREVENTIVA)
                .estado(EstadoCapa.CERRADA)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaCierre(LocalDateTime.now())
                .build();
        when(capaService.cerrar(3L)).thenReturn(respuesta);

        mockMvc.perform(patch("/api/calidad/capas/3/cerrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    void adjuntarArchivoRetorna201() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "plan.txt",
                "text/plain",
                "contenido".getBytes()
        );

        when(capaService.adjuntarArchivo(eq(4L), any(), any(), any()))
                .thenReturn(CapaArchivoDTO.builder()
                        .id(11L)
                        .nombreArchivo("plan_guardado.txt")
                        .nombreVisible("plan.txt")
                        .build());

        mockMvc.perform(multipart("/api/calidad/capas/4/archivos")
                        .file(archivo)
                        .param("nombreVisible", "plan.txt"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
