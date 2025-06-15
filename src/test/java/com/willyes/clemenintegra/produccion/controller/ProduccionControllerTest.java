package com.willyes.clemenintegra.produccion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.service.OrdenProduccionService;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdenProduccionController.class)
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class ProduccionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrdenProduccionService ordenProduccionService;

    private OrdenProduccionRequestDTO validRequest;
    private OrdenProduccion ordenEntity;

    @BeforeEach
    void setUp() {
        validRequest = OrdenProduccionRequestDTO.builder()
                .loteProduccion("LOTE-TEST-001")
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(2))
                .cantidadProgramada(50)
                .cantidadProducida(0)
                .estado("CREADA")
                .productoId(1L)
                .responsableId(2L)
                .build();

        Producto prod = new Producto();
        prod.setId(1L);
        prod.setNombre("Producto A");
        Usuario resp = new Usuario();
        resp.setId(2L);
        resp.setNombreCompleto("Usuario Test");

        ordenEntity = OrdenProduccion.builder()
                .id(10L)
                .loteProduccion("LOTE-TEST-001")
                .fechaInicio(validRequest.getFechaInicio())
                .fechaFin(validRequest.getFechaFin())
                .cantidadProgramada(validRequest.getCantidadProgramada())
                .cantidadProducida(validRequest.getCantidadProducida())
                .estado(EstadoProduccion.CREADA)
                .producto(prod)
                .responsable(resp)
                .build();
    }

    @Test
    void registrarOrdenProduccionValida_retornaCreated() throws Exception {
        given(ordenProduccionService.guardarConValidacionStock(any()))
                .willReturn(ordenEntity);

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loteProduccion").value("LOTE-TEST-001"))
                .andExpect(jsonPath("$.nombreProducto").value("Producto A"))
                .andExpect(jsonPath("$.nombreResponsable").value("Usuario Test"));

        ArgumentCaptor<OrdenProduccion> captor = ArgumentCaptor.forClass(OrdenProduccion.class);
        verify(ordenProduccionService).guardarConValidacionStock(captor.capture());
        OrdenProduccion enviado = captor.getValue();
        assertThat(enviado.getProducto().getId()).isEqualTo(1L);
        assertThat(enviado.getResponsable().getId()).isEqualTo(2L);
        assertThat(enviado.getLoteProduccion()).isEqualTo("LOTE-TEST-001");
    }

    @Test
    void registrarOrdenProduccion_sinStock_retornaError() throws Exception {
        given(ordenProduccionService.guardarConValidacionStock(any()))
                .willThrow(new IllegalStateException("Stock insuficiente"));

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void registrarOrdenProduccion_insumoInexistente_retornarBadRequest() throws Exception {
        given(ordenProduccionService.guardarConValidacionStock(any()))
                .willThrow(new IllegalArgumentException("Insumo no encontrado"));

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insumo no encontrado"));
    }

    @Test
    void obtenerOrdenPorId_existente() throws Exception {
        given(ordenProduccionService.buscarPorId(10L)).willReturn(Optional.of(ordenEntity));

        mockMvc.perform(get("/api/produccion/ordenes/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.loteProduccion").value("LOTE-TEST-001"));
    }

    @Test
    void obtenerOrdenPorId_noExiste() throws Exception {
        given(ordenProduccionService.buscarPorId(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/produccion/ordenes/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarTodasOrdenes() throws Exception {
        given(ordenProduccionService.listarTodas()).willReturn(List.of(ordenEntity));

        mockMvc.perform(get("/api/produccion/ordenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void registrarOrdenProduccion_fechasInvalidas_retornarBadRequest() throws Exception {
        OrdenProduccionRequestDTO invalid = OrdenProduccionRequestDTO.builder()
                .loteProduccion("LOTE-TEST-002")
                .fechaInicio(LocalDateTime.now().plusDays(1))
                .fechaFin(LocalDateTime.now())
                .cantidadProgramada(10)
                .cantidadProducida(0)
                .estado("CREADA")
                .productoId(1L)
                .responsableId(2L)
                .build();

        given(ordenProduccionService.guardarConValidacionStock(any()))
                .willThrow(new IllegalArgumentException("Fechas de producción inválidas"));

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fechas de producción inválidas"));
    }
}
