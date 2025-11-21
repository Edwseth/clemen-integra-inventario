package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.repository.ControlEmpaqueLoteRepository;
import com.willyes.clemenintegra.produccion.repository.ControlProcesoProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ObservacionProcesoRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.BatchRecordService;
import com.willyes.clemenintegra.produccion.service.ReporteBatchRecordService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(BatchRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@SpringJUnitConfig(classes = BatchRecordControllerTest.TestSecurityConfig.class)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class BatchRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchRecordService batchRecordService;
    @MockBean
    private OrdenProduccionRepository ordenProduccionRepository;
    @MockBean
    private ControlProcesoProduccionRepository controlProcesoProduccionRepository;
    @MockBean
    private ControlEmpaqueLoteRepository controlEmpaqueLoteRepository;
    @MockBean
    private ObservacionProcesoRepository observacionProcesoRepository;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private ReporteBatchRecordService reporteBatchRecordService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/batch-record/{id} devuelve op y fórmula")
    void obtenerBatchRecord() throws Exception {
        BatchRecordDTO dto = new BatchRecordDTO();
        BatchRecordDTO.OpDTO opDTO = new BatchRecordDTO.OpDTO();
        opDTO.id = 1L;
        opDTO.codigoOrden = "OP-1";
        dto.op = opDTO;
        BatchRecordDTO.FormulaDTO formulaDTO = new BatchRecordDTO.FormulaDTO();
        formulaDTO.version = "v1";
        formulaDTO.detalles = Collections.emptyList();
        dto.formula = formulaDTO;

        when(batchRecordService.buildByOrdenProduccion(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/produccion/batch-record/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.op.codigoOrden").value("OP-1"))
                .andExpect(jsonPath("$.formula.version").value("v1"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("GET /api/produccion/batch-record/{id} permite consulta a jefe de calidad")
    void obtenerBatchRecordRolCalidad() throws Exception {
        BatchRecordDTO dto = new BatchRecordDTO();
        dto.op = new BatchRecordDTO.OpDTO();
        dto.op.codigoOrden = "OP-2";
        when(batchRecordService.buildByOrdenProduccion(2L)).thenReturn(dto);

        mockMvc.perform(get("/api/produccion/batch-record/{id}", 2L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.op.codigoOrden").value("OP-2"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/batch-record/{id} devuelve 404 cuando no existe")
    void obtenerBatchRecordNoExiste() throws Exception {
        when(batchRecordService.buildByOrdenProduccion(anyLong()))
                .thenThrow(new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "ORDEN_NO_ENCONTRADA"));

        mockMvc.perform(get("/api/produccion/batch-record/{id}", 999L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.RECURSO_NO_ENCONTRADO.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/batch-record/{id}/pdf devuelve PDF con headers")
    void exportarPdfBatchRecord() throws Exception {
        BatchRecordDTO dto = new BatchRecordDTO();
        BatchRecordDTO.OpDTO opDTO = new BatchRecordDTO.OpDTO();
        opDTO.id = 5L;
        opDTO.codigoOrden = "OP-123";
        dto.op = opDTO;
        when(batchRecordService.buildByOrdenProduccion(5L)).thenReturn(dto);
        when(reporteBatchRecordService.generarPdfBatchRecord(5L)).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/produccion/batch-record/{id}/pdf", 5L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=batch-record-OP-123.pdf"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("GET /api/produccion/batch-record/{id}/pdf permite acceso a jefe de calidad")
    void exportarPdfBatchRecordRolCalidad() throws Exception {
        BatchRecordDTO dto = new BatchRecordDTO();
        BatchRecordDTO.OpDTO opDTO = new BatchRecordDTO.OpDTO();
        opDTO.id = 7L;
        opDTO.codigoOrden = "OP-777";
        dto.op = opDTO;
        when(batchRecordService.buildByOrdenProduccion(7L)).thenReturn(dto);
        when(reporteBatchRecordService.generarPdfBatchRecord(7L)).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/produccion/batch-record/{id}/pdf", 7L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=batch-record-OP-777.pdf"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("POST /api/produccion/batch-record/{id}/decision devuelve 204")
    void decidirBatchRecord() throws Exception {
        String body = "{\"decision\":\"APROBADO\",\"observacionesCalidad\":\"Listo\"}";

        mockMvc.perform(post("/api/produccion/batch-record/{id}/decision", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(batchRecordService).decidirBatchRecord(eq(10L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/batch-record/{id}/decision requiere rol de calidad")
    void decidirBatchRecordNoAutorizado() throws Exception {
        String body = "{\"decision\":\"APROBADO\"}";

        mockMvc.perform(post("/api/produccion/batch-record/{id}/decision", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        verify(batchRecordService, never()).decidirBatchRecord(anyLong(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("POST /api/produccion/batch-record/{id}/controles-proceso es rechazado para jefe de calidad")
    void guardarControlesProceso_conRolCalidad_devuelve403() throws Exception {
        mockMvc.perform(post("/api/produccion/batch-record/{id}/controles-proceso", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());
    }
}
