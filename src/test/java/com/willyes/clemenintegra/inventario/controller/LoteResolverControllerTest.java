package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ProductoPorLoteDTO;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoteResolverController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoteResolverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoteProductoService loteProductoService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void retornaProductoCuandoElLoteExiste() throws Exception {
        ProductoPorLoteDTO dto = new ProductoPorLoteDTO(1L, "SKU-123", "Producto Demo");
        when(loteProductoService.resolverProductoPorLote(eq("L-001"), eq(99L))).thenReturn(dto);

        mockMvc.perform(get("/api/inventario/lotes/resolver-producto")
                        .param("codigoLote", "L-001")
                        .param("ordenProduccionId", "99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-123"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto Demo"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    void retornaErrorControladoCuandoNoExisteLote() throws Exception {
        when(loteProductoService.resolverProductoPorLote(eq("NOPE"), eq(1L)))
                .thenThrow(new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO, "LOTE_NO_ENCONTRADO"));

        mockMvc.perform(get("/api/inventario/lotes/resolver-producto")
                        .param("codigoLote", "NOPE")
                        .param("ordenProduccionId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").value("LOTE_NO_ENCONTRADO"));

        Mockito.verify(loteProductoService).resolverProductoPorLote("NOPE", 1L);
    }
}
