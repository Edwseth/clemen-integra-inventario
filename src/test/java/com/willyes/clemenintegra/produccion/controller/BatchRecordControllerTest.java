package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.repository.ControlEmpaqueLoteRepository;
import com.willyes.clemenintegra.produccion.repository.ControlProcesoProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ObservacionProcesoRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.BatchRecordService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
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
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

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
}
