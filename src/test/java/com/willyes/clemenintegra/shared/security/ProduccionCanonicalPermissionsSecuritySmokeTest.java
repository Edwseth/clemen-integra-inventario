package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.controller.BatchRecordController;
import com.willyes.clemenintegra.produccion.controller.OrdenProduccionController;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.ControlEmpaqueLoteRepository;
import com.willyes.clemenintegra.produccion.repository.ControlProcesoProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ObservacionProcesoRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.BatchRecordService;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaService;
import com.willyes.clemenintegra.produccion.service.OrdenProduccionService;
import com.willyes.clemenintegra.produccion.service.ReporteBatchRecordService;
import com.willyes.clemenintegra.produccion.service.ReporteOrdenProduccionService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrdenProduccionController.class, BatchRecordController.class})
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
@Import(SecurityConfig.class)
class ProduccionCanonicalPermissionsSecuritySmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdenProduccionService ordenProduccionService;
    @MockBean
    private ReporteOrdenProduccionService reporteOrdenProduccionService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private MovimientoInventarioService movimientoInventarioService;
    @MockBean
    private ChecklistEtapaService checklistEtapaService;
    @MockBean
    private OrdenProduccionRepository ordenProduccionRepository;

    @MockBean
    private BatchRecordService batchRecordService;
    @MockBean
    private ControlProcesoProduccionRepository controlProcesoProduccionRepository;
    @MockBean
    private ControlEmpaqueLoteRepository controlEmpaqueLoteRepository;
    @MockBean
    private ObservacionProcesoRepository observacionProcesoRepository;
    @MockBean
    private ReporteBatchRecordService reporteBatchRecordService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void configureFilters() throws ServletException, IOException {
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
    @WithMockUser(authorities = "PROD_READ")
    void prodReadPermiteListarYVerOrdenes() throws Exception {
        when(ordenProduccionService.listarPaginado(any(), any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());
        when(ordenProduccionService.buscarPorId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/produccion/ordenes"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/produccion/ordenes/{id}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "PROD_WRITE")
    void prodWritePermiteCrearOrdenProduccion() throws Exception {
        when(ordenProduccionService.crearOrden(any()))
                .thenReturn(ResultadoValidacionOrdenDTO.builder().esValida(true).build());

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType("application/json")
                        .content("""
                                {
                                  "fechaProgramada": "2099-01-29T10:00:00",
                                  "cantidadProgramada": 10,
                                  "estado": "CREADA",
                                  "productoId": 1,
                                  "responsableId": 1,
                                  "unidadMedidaSimbolo": "kg",
                                  "confirmacionHomeopatico": false
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "PROD_WORKFLOW_START")
    void prodWorkflowStartPermiteIniciarEtapa() throws Exception {
        when(ordenProduccionService.buscarPorId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/iniciar", 1L, 2L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "PROD_WORKFLOW_FINISH")
    void prodWorkflowFinishPermiteFinalizarEtapa() throws Exception {
        when(ordenProduccionService.finalizar(any(), any())).thenReturn(OrdenProduccion.builder()
                .id(1L)
                .codigoOrden("OP-1")
                .estado(EstadoProduccion.FINALIZADA)
                .build());

        mockMvc.perform(put("/api/produccion/ordenes/{id}/finalizar", 1L)
                        .contentType("application/json")
                        .content("{\"cantidadProducida\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"PROD_READ", "PROD_EXPORT", "PROD_OP_EXPORT"})
    void prodExportPermiteExportarOrdenes() throws Exception {
        // /api/produccion/ordenes/export/pdf requiere PROD_READ por SecurityConfig y PROD_EXPORT|PROD_OP_EXPORT por @PreAuthorize.
        when(ordenProduccionService.listar(anyString(), anyString(), any(EstadoProduccion.class), anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(reporteOrdenProduccionService.generarPdfOrdenesProduccion(any())).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/produccion/ordenes/export/pdf"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PROD_DECIDE")
    void prodDecidePermiteDecisionBatchRecord() throws Exception {
        mockMvc.perform(post("/api/produccion/calidad/batch-record/{id}/decision", 10L)
                        .contentType("application/json")
                        .content("{\"decision\":\"APROBADO\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = {"PROD_READ", "PROD_EXPORT", "PROD_BATCH_RECORD_EXPORT"})
    void prodExportPermiteExportarBatchRecord() throws Exception {
        // /api/produccion/batch-record/{id}/pdf cae en /api/produccion/** (PROD_READ en SecurityConfig) y además exige export por @PreAuthorize.
        when(batchRecordService.buildByOrdenProduccion(1L)).thenReturn(new BatchRecordDTO());
        when(reporteBatchRecordService.generarPdfBatchRecord(1L)).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/produccion/batch-record/{id}/pdf", 1L))
                .andExpect(status().isOk());
    }
}
