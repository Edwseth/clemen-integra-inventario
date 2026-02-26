package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.ProductoCalidadUpdateDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.support.TestMethodSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductoCalidadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestMethodSecurityConfig.class)
class ProductoCalidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductoService productoService;


    @Test
    @WithMockUser(authorities = "INV_READ")
    void jefeCalidadPuedeActualizarCamposCalidad() throws Exception {
        long productoId = 10L;
        ProductoCalidadUpdateDTO dto = new ProductoCalidadUpdateDTO(true, false, true);
        ProductoResponseDTO response = ProductoResponseDTO.builder()
                .id(productoId)
                .requiereAnalisisFisico(true)
                .requiereAnalisisQuimico(false)
                .requiereAnalisisMicrobiologico(true)
                .build();

        when(productoService.actualizarCamposCalidad(eq(productoId), any(ProductoCalidadUpdateDTO.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/inventario/productos/{id}/calidad", productoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereAnalisisFisico").value(true))
                .andExpect(jsonPath("$.requiereAnalisisQuimico").value(false))
                .andExpect(jsonPath("$.requiereAnalisisMicrobiologico").value(true));
    }

    @Test
    @WithMockUser(authorities = "INV_WRITE")
    void rolNoAutorizadoRecibeForbidden() throws Exception {
        long productoId = 10L;
        ProductoCalidadUpdateDTO dto = new ProductoCalidadUpdateDTO(true, true, true);

        mockMvc.perform(patch("/api/inventario/productos/{id}/calidad", productoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "INV_READ")
    void idInexistenteDevuelveNotFound() throws Exception {
        when(productoService.actualizarCamposCalidad(eq(999999L), any(ProductoCalidadUpdateDTO.class)))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "PRODUCTO_NO_ENCONTRADO"));
        ProductoCalidadUpdateDTO dto = new ProductoCalidadUpdateDTO(true, true, false);

        mockMvc.perform(patch("/api/inventario/productos/{id}/calidad", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }
}
