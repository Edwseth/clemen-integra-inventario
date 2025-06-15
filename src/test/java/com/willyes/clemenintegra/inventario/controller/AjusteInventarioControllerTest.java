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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class AjusteInventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

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
    void registrarAjusteNegativoSinStockDebeFallar() throws Exception {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("-1000.00"))
                .motivo("Ajuste sin stock")
                .productoId(productoId)
                .almacenId(almacenId)
                .usuarioId(usuarioId)
                .build();

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay suficiente stock disponible"));
    }

    @Test
    void registrarAjusteConCantidadCeroDebeFallar() throws Exception {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(BigDecimal.ZERO)
                .motivo("Sin cambios")
                .productoId(productoId)
                .almacenId(almacenId)
                .usuarioId(usuarioId)
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
                .productoId(9999L)
                .almacenId(almacenId)
                .usuarioId(usuarioId)
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
                .productoId(productoId)
                .almacenId(almacenId)
                .usuarioId(9999L)
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
                .productoId(productoId)
                .almacenId(9999L)
                .usuarioId(usuarioId)
                .build();

        mockMvc.perform(post("/api/inventario/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Almacén no encontrado"));
    }

    @Test
    void obtenerAjustePorIdExistenteDebeRetornarOk() throws Exception {
        mockMvc.perform(get("/api/inventario/ajustes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void obtenerAjustePorIdNoExistenteDebeRetornarNotFound() throws Exception {
        mockMvc.perform(get("/api/inventario/ajustes/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarTodosLosAjustesDebeRetornarLista() throws Exception {
        mockMvc.perform(get("/api/inventario/ajustes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }
}
