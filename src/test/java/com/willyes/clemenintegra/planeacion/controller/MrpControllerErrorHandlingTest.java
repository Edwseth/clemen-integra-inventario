package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.planeacion.service.MrpReporteService;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MrpController.class)
@AutoConfigureMockMvc(addFilters = false)
class MrpControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MrpService mrpService;

    @MockBean
    private PlanProduccionService planProduccionService;

    @MockBean
    private MrpReporteService mrpReporteService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    void obtenerCorridaSinUnidadMedidaRetorna422() throws Exception {
        when(mrpService.obtenerCorrida(7L))
                .thenThrow(new CustomBusinessException(ApiErrorCode.PRODUCTO_SIN_UNIDAD_MEDIDA,
                        "El producto SKU-123 no tiene unidad de medida configurada"));

        mockMvc.perform(get("/api/mrp/corridas/7"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.PRODUCTO_SIN_UNIDAD_MEDIDA.getCode()));
    }
}
