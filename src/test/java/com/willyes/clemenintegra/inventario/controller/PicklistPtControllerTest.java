package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.PicklistPtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PicklistPtController.class)
@AutoConfigureMockMvc(addFilters = false)
class PicklistPtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PicklistPtService picklistPtService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(username = "carlos", authorities = "ROL_JEFE_ALMACENES")
    void pdfDevuelveContentTypePdf() throws Exception {
        when(picklistPtService.generarPdf(5L)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/inventario/picklists-pt/5/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
