package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class AjusteInventarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // IDs precargados desde data.sql
    private final Long productoId = 1L;
    private final Long almacenId = 1L;
    private final Long usuarioId = 1L;

    @Test
    void registrarAjustePositivoDebeSerExitoso() throws Exception {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("10.00"))
                .motivo("Ingreso por revisión")
                .observaciones("Ajuste inventario físico positivo")
                .productoId(productoId)
                .almacenId(almacenId)
                .usuarioId(usuarioId)
                .build();

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(10.00))
                .andExpect(jsonPath("$.motivo").value("Ingreso por revisión"))
                .andExpect(jsonPath("$.productoNombre").value("Producto Test"));
    }

    @Test
    void registrarAjusteNegativoDebeSerExitoso() throws Exception {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("-7.50"))
                .motivo("Salida por pérdida")
                .productoId(productoId)
                .almacenId(almacenId)
                .usuarioId(usuarioId)
                .build();

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(-7.50))
                .andExpect(jsonPath("$.motivo").value("Salida por pérdida"));
    }

    @Test
    void registrarAjusteConCantidadCeroDebeFallar() throws Exception {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(BigDecimal.ZERO)
                .motivo("Sin cambios")
                .productoId(1L)
                .almacenId(1L)
                .usuarioId(1L)
                .build();

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La cantidad no puede ser cero"));
    }

    @Test
    void registrarAjusteConProductoInexistenteDebeFallar() throws Exception {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("5.00"))
                .motivo("Prueba producto inexistente")
                .productoId(9999L)  // ID que no existe
                .almacenId(1L)
                .usuarioId(1L)
                .build();

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Producto no encontrado"));
    }

    @Test
    void registrarAjusteConUsuarioInvalidoDebeFallar() throws Exception {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("3.00"))
                .motivo("Usuario no válido")
                .productoId(1L)
                .almacenId(1L)
                .usuarioId(9999L)  // ID inexistente
                .build();

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void registrarAjusteConAlmacenInvalidoDebeFallar() throws Exception {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("2.50"))
                .motivo("Almacén no válido")
                .productoId(1L)
                .almacenId(9999L)  // ID inexistente
                .usuarioId(1L)
                .build();

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Almacén no encontrado"));
    }

}
