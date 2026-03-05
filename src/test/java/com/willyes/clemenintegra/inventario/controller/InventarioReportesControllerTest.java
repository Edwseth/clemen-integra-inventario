package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventarioReportesController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventarioReportesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteInventarioService reporteInventarioService;

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void listarConteosAjuste_debeAplicarFiltroCuandoLlegaSoloDiferenciasTrue() throws Exception {
        when(reporteInventarioService.listarConteosAjuste(any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/inventario/reportes/conteos-ajuste")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-03-05")
                        .param("soloDiferencias", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> soloConDiferenciaCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(reporteInventarioService).listarConteosAjuste(
                any(), any(), any(), any(), soloConDiferenciaCaptor.capture(), any(), any(), any(), any());

        assertThat(soloConDiferenciaCaptor.getValue()).isTrue();
    }

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void listarConteosAjuste_debeMantenerCompatibilidadConSoloConDiferencia() throws Exception {
        when(reporteInventarioService.listarConteosAjuste(any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/inventario/reportes/conteos-ajuste")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-03-05")
                        .param("soloConDiferencia", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> soloConDiferenciaCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(reporteInventarioService).listarConteosAjuste(
                any(), any(), any(), any(), soloConDiferenciaCaptor.capture(), any(), any(), any(), any());

        assertThat(soloConDiferenciaCaptor.getValue()).isTrue();
    }

    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void listarConteosAjuste_cuandoNoLlegaParametroDebeEnviarFalse() throws Exception {
        when(reporteInventarioService.listarConteosAjuste(any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/inventario/reportes/conteos-ajuste")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-03-05"))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> soloConDiferenciaCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(reporteInventarioService).listarConteosAjuste(
                any(), any(), any(), any(), soloConDiferenciaCaptor.capture(), any(), any(), any(), any());

        assertThat(soloConDiferenciaCaptor.getValue()).isFalse();
    }
    @Test
    @WithMockUser(authorities = "INV_REPORTES_EXPORT")
    void listarConteosAjuste_debeEnviarParametrosDeOrdenamiento() throws Exception {
        when(reporteInventarioService.listarConteosAjuste(any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/inventario/reportes/conteos-ajuste")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-03-05")
                        .param("page", "0")
                        .param("size", "100")
                        .param("sortField", "fechaAplicacion")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk());

        verify(reporteInventarioService).listarConteosAjuste(
                any(), any(), any(), any(), anyBoolean(),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.eq("fechaAplicacion"),
                org.mockito.ArgumentMatchers.eq("asc")
        );
    }

}
