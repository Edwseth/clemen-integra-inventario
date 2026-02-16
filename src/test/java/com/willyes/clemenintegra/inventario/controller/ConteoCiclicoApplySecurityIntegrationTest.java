package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
class ConteoCiclicoApplySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConteoCiclicoService conteoCiclicoService;

    @Test
    void aplicarConPermisoApplyResponde200() throws Exception {
        ConteoCiclicoResponseDTO response = ConteoCiclicoResponseDTO.builder()
                .id(77L)
                .estado(EstadoConteoCiclico.APLICADO)
                .build();
        when(conteoCiclicoService.aplicar(anyLong(), eq("k1"))).thenReturn(response);

        mockMvc.perform(post("/api/inventario/conteos/77/aplicar")
                        .header("Idempotency-Key", "k1")
                        .with(SecurityMockMvcRequestPostProcessors.user("contador")
                                .authorities(() -> "INV_CONTEOS_APPLY")))
                .andExpect(status().isOk());
    }

    @Test
    void aplicarSinPermisoApplyResponde403() throws Exception {
        mockMvc.perform(post("/api/inventario/conteos/77/aplicar")
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe")
                                .authorities(() -> "INV_CONTEOS_WRITE")))
                .andExpect(status().isForbidden());
    }
}
