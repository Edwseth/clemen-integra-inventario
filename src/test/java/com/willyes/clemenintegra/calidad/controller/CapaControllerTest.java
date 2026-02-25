package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.CapaArchivoDTO;
import com.willyes.clemenintegra.calidad.dto.CapaArchivoDescargaDTO;
import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.TipoCapa;
import com.willyes.clemenintegra.calidad.service.CapaService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.willyes.clemenintegra.shared.security.testsupport.WithTestSuperAdmin;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CapaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CapaControllerTest.MethodSecurityTestConfig.class, GlobalExceptionHandler.class})
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
    @WithTestSuperAdmin
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
    @WithTestSuperAdmin
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
    @WithTestSuperAdmin
    void crearCapaAceptaDescripcionComoObservaciones() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2024, 5, 1, 10, 0);
        LocalDateTime limite = inicio.plusDays(5);
        CapaDTO respuesta = CapaDTO.builder()
                .id(4L)
                .noConformidadId(9L)
                .noConformidadCodigo("NC-9")
                .responsableId(5L)
                .responsableNombre("Responsable QA")
                .tipo(TipoCapa.CORRECTIVA)
                .estado(EstadoCapa.ACTIVA)
                .fechaInicio(inicio)
                .fechaLimite(limite)
                .observaciones("Texto desde servicio")
                .build();

        ArgumentCaptor<CapaDTO> captor = ArgumentCaptor.forClass(CapaDTO.class);
        when(capaService.crear(any(CapaDTO.class))).thenAnswer(invocation -> {
            CapaDTO dto = invocation.getArgument(0);
            return CapaDTO.builder()
                    .id(respuesta.getId())
                    .noConformidadId(respuesta.getNoConformidadId())
                    .noConformidadCodigo(respuesta.getNoConformidadCodigo())
                    .responsableId(respuesta.getResponsableId())
                    .responsableNombre(respuesta.getResponsableNombre())
                    .tipo(respuesta.getTipo())
                    .estado(respuesta.getEstado())
                    .fechaInicio(respuesta.getFechaInicio())
                    .fechaLimite(respuesta.getFechaLimite())
                    .observaciones(dto.getObservaciones())
                    .build();
        });

        mockMvc.perform(post("/api/calidad/capas")
                        .contentType(APPLICATION_JSON)
                        .content("{\"noConformidadId\":9,\"tipo\":\"CORRECTIVA\",\"responsableId\":5," +
                                "\"fechaInicio\":\"2024-05-01T10:00:00\",\"fechaLimite\":\"2024-05-06T10:00:00\"," +
                                "\"descripcion\":\"Detalle observado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.observaciones").value("Detalle observado"))
                .andExpect(jsonPath("$.fechaLimite").value("2024-05-06T10:00:00"))
                .andExpect(jsonPath("$.noConformidadCodigo").value("NC-9"))
                .andExpect(jsonPath("$.responsableNombre").value("Responsable QA"));

        verify(capaService).crear(captor.capture());
        assertThat(captor.getValue().getObservaciones()).isEqualTo("Detalle observado");
    }

    @Test
    @WithTestSuperAdmin
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
    @WithTestSuperAdmin
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

    @Test
    @WithTestSuperAdmin
    void flujoAdjuntosListaYDescarga() throws Exception {
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
                        .contentType("text/plain")
                        .build());
        when(capaService.listarArchivos(4L)).thenReturn(List.of(
                CapaArchivoDTO.builder()
                        .id(11L)
                        .nombreArchivo("plan_guardado.txt")
                        .nombreVisible("plan legible.txt")
                        .build()));
        when(capaService.descargarArchivo(4L, 11L))
                .thenReturn(CapaArchivoDescargaDTO.builder()
                        .contenido("contenido".getBytes())
                        .nombreArchivo("plan legible.txt")
                        .contentType("text/plain")
                        .build());

        mockMvc.perform(multipart("/api/calidad/capas/4/archivos")
                        .file(archivo)
                        .param("nombreVisible", "plan.txt"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11));

        mockMvc.perform(get("/api/calidad/capas/4/archivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));

        mockMvc.perform(get("/api/calidad/capas/4/archivos/11/descargar"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plan legible.txt\""))
                .andExpect(content().bytes("contenido".getBytes()))
                .andExpect(header().string("Content-Type", "text/plain"));
    }

    @Test
    @WithTestSuperAdmin
    void descargarArchivoNoEncontradoDevuelve404() throws Exception {
        when(capaService.descargarArchivo(9L, 77L))
                .thenThrow(new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "Archivo no encontrado"));

        mockMvc.perform(get("/api/calidad/capas/9/archivos/77/descargar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
