package com.willyes.clemenintegra.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.dto.AlmacenRequestDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.willyes.clemenintegra.inventario.config.TestSecurityConfig.class)
class AlmacenControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AlmacenRepository almacenRepository;

    @BeforeEach
    void setUp() {
        almacenRepository.deleteAll();
    }

    @Test
    void crearAlmacenValido_debeRetornarCreated() throws Exception {
        AlmacenRequestDTO dto = AlmacenRequestDTO.builder()
                .nombre("Almacen Principal")
                .ubicacion("Zona A")
                .tipo(TipoAlmacen.PRINCIPAL)
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .build();

        mockMvc.perform(post("/api/almacenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Almacen Principal"));
    }

    @Test
    void crearAlmacenDuplicado_debeRetornarConflict() throws Exception {
        almacenRepository.save(Almacen.builder()
                .nombre("Duplicado")
                .ubicacion("B1")
                .tipo(TipoAlmacen.PRINCIPAL)
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .build());

        AlmacenRequestDTO dto = AlmacenRequestDTO.builder()
                .nombre("Duplicado")
                .ubicacion("B2")
                .tipo(TipoAlmacen.PRINCIPAL)
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .build();

        mockMvc.perform(post("/api/almacenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void crearAlmacenInvalido_debeRetornarBadRequest() throws Exception {
        AlmacenRequestDTO dto = AlmacenRequestDTO.builder()
                .nombre("")
                .ubicacion("B1")
                .tipo(TipoAlmacen.PRINCIPAL)
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .build();

        mockMvc.perform(post("/api/almacenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerAlmacenPorId_debeRetornarAlmacen() throws Exception {
        Almacen almacen = almacenRepository.save(Almacen.builder()
                .nombre("Existente")
                .ubicacion("C1")
                .tipo(TipoAlmacen.PRINCIPAL)
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .build());

        mockMvc.perform(get("/api/almacenes/{id}", almacen.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(almacen.getId()))
                .andExpect(jsonPath("$.nombre").value("Existente"));
    }

    @Test
    void obtenerAlmacenNoExistente_debeRetornarNotFound() throws Exception {
        mockMvc.perform(get("/api/almacenes/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarTodosLosAlmacenes_debeRetornarLista() throws Exception {
        almacenRepository.save(Almacen.builder()
                .nombre("A1")
                .ubicacion("U1")
                .tipo(TipoAlmacen.PRINCIPAL)
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .build());

        mockMvc.perform(get("/api/almacenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }
}
